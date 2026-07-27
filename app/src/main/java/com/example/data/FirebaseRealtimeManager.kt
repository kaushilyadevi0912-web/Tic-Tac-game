package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FirebaseRealtimeManager(context: Context) {

    private val inMemoryRooms = ConcurrentHashMap<String, OnlineRoomData>()
    private val inMemoryFlows = ConcurrentHashMap<String, MutableStateFlow<OnlineRoomData?>>()

    private val dbUrl = "https://neon-tictactoe-c9439-default-rtdb.firebaseio.com"

    private val database: FirebaseDatabase? = try {
        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:153199357390:android:43d91f112d99c647ab0f9f")
                .setApiKey("AIzaSyCgatxG0axwpv1_8BSiqgoTg9fJkW7R7P4")
                .setDatabaseUrl(dbUrl)
                .setProjectId("neon-tictactoe-c9439")
                .setStorageBucket("neon-tictactoe-c9439.firebasestorage.app")
                .build()
            FirebaseApp.initializeApp(context, options)
        } else {
            FirebaseApp.getInstance()
        }
        FirebaseDatabase.getInstance(app, dbUrl)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private val roomsRef: DatabaseReference? = try {
        database?.getReference("rooms")
    } catch (e: Exception) {
        null
    }

    val myPlayerId: String = UUID.randomUUID().toString().take(8)

    fun generate3DigitCode(): String {
        return (100..999).random().toString()
    }

    private fun getOrCreateFlow(roomCode: String): MutableStateFlow<OnlineRoomData?> {
        return inMemoryFlows.getOrPut(roomCode) {
            MutableStateFlow(inMemoryRooms[roomCode])
        }
    }

    private fun notifyLocalFlow(roomCode: String, data: OnlineRoomData) {
        inMemoryRooms[roomCode] = data
        getOrCreateFlow(roomCode).value = data
    }

    fun createRoom(
        roomCode: String,
        gridSize: Int = 3,
        hostName: String = "Player 1",
        onSuccess: (OnlineRoomData) -> Unit,
        onError: (String) -> Unit
    ) {
        val totalCells = gridSize * gridSize
        val cleanHostName = hostName.ifBlank { "Player 1" }
        val room = OnlineRoomData(
            roomCode = roomCode,
            status = "WAITING",
            playerHostId = myPlayerId,
            playerGuestId = null,
            hostName = cleanHostName,
            guestName = "Player 2",
            activePlayer = "O",
            gridSize = gridSize,
            board = List(totalCells) { "" },
            winner = null,
            isDraw = false
        )

        notifyLocalFlow(roomCode, room)

        val ref = roomsRef
        if (ref != null) {
            ref.child(roomCode).setValue(room)
                .addOnSuccessListener {
                    onSuccess(room)
                }
                .addOnFailureListener {
                    // Fallback to local memory if network fails
                    onSuccess(room)
                }
        } else {
            onSuccess(room)
        }
    }

    fun joinRoom(
        roomCode: String,
        guestName: String = "Player 2",
        onSuccess: (OnlineRoomData) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanGuestName = guestName.ifBlank { "Player 2" }
        val localRoom = inMemoryRooms[roomCode]
        if (localRoom != null) {
            if (localRoom.playerHostId == myPlayerId) {
                onSuccess(localRoom)
                return
            }
            if (localRoom.playerGuestId != null && localRoom.playerGuestId.isNotEmpty() && localRoom.status == "PLAYING" && localRoom.playerGuestId != myPlayerId) {
                onError("Room $roomCode is already full!")
                return
            }
            val updated = localRoom.copy(playerGuestId = myPlayerId, guestName = cleanGuestName, status = "PLAYING")
            notifyLocalFlow(roomCode, updated)

            roomsRef?.child(roomCode)?.updateChildren(
                mapOf("playerGuestId" to myPlayerId, "guestName" to cleanGuestName, "status" to "PLAYING")
            )

            onSuccess(updated)
            return
        }

        val ref = roomsRef
        if (ref == null) {
            onError("Room code $roomCode not found. Please create a room first.")
            return
        }

        val roomRef = ref.child(roomCode)
        roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onError("Room code $roomCode does not exist!")
                    return
                }

                val room = snapshot.getValue(OnlineRoomData::class.java)
                if (room == null) {
                    onError("Invalid room data")
                    return
                }

                if (room.playerHostId == myPlayerId) {
                    notifyLocalFlow(roomCode, room)
                    onSuccess(room)
                    return
                }

                if (room.playerGuestId != null && room.playerGuestId.isNotEmpty() && room.status == "PLAYING" && room.playerGuestId != myPlayerId) {
                    onError("Room $roomCode is already full!")
                    return
                }

                val now = System.currentTimeMillis()
                val updatedRoom = room.copy(
                    playerGuestId = myPlayerId,
                    guestName = cleanGuestName,
                    status = "PLAYING",
                    turnStartTime = now
                )
                notifyLocalFlow(roomCode, updatedRoom)

                val updatedMap = mapOf<String, Any?>(
                    "playerGuestId" to myPlayerId,
                    "guestName" to cleanGuestName,
                    "status" to "PLAYING",
                    "turnStartTime" to now
                )

                roomRef.updateChildren(updatedMap)
                    .addOnSuccessListener {
                        onSuccess(updatedRoom)
                    }
                    .addOnFailureListener {
                        onSuccess(updatedRoom)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

    fun observeRoom(roomCode: String): Flow<OnlineRoomData?> {
        val flow = getOrCreateFlow(roomCode)
        val ref = roomsRef ?: return flow

        return callbackFlow {
            val roomRef = ref.child(roomCode)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        inMemoryRooms.remove(roomCode)
                        val f = inMemoryFlows[roomCode]
                        if (f != null) f.value = null
                        trySend(null)
                        return
                    }
                    val room = snapshot.getValue(OnlineRoomData::class.java)
                    if (room != null) {
                        notifyLocalFlow(roomCode, room)
                        trySend(room)
                    } else {
                        inMemoryRooms.remove(roomCode)
                        trySend(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(flow.value)
                }
            }

            roomRef.addValueEventListener(listener)

            awaitClose {
                roomRef.removeEventListener(listener)
            }
        }
    }

    fun makeMove(
        roomCode: String,
        board: List<String>,
        nextPlayer: String,
        winner: String?,
        isDraw: Boolean,
        scoreO: Int? = null,
        scoreX: Int? = null
    ) {
        val now = System.currentTimeMillis()
        val current = inMemoryRooms[roomCode]
        val newScoreO = scoreO ?: ((current?.scoreO ?: 0) + if (winner == "O") 1 else 0)
        val newScoreX = scoreX ?: ((current?.scoreX ?: 0) + if (winner == "X") 1 else 0)

        if (current != null) {
            val status = if (winner != null || isDraw) "FINISHED" else "PLAYING"
            val updated = current.copy(
                board = board,
                activePlayer = nextPlayer,
                winner = winner,
                isDraw = isDraw,
                status = status,
                scoreO = newScoreO,
                scoreX = newScoreX,
                turnStartTime = now
            )
            notifyLocalFlow(roomCode, updated)
        }

        val ref = roomsRef ?: return
        val status = if (winner != null || isDraw) "FINISHED" else "PLAYING"
        val updates = mapOf(
            "board" to board,
            "activePlayer" to nextPlayer,
            "winner" to winner,
            "isDraw" to isDraw,
            "status" to status,
            "scoreO" to newScoreO,
            "scoreX" to newScoreX,
            "turnStartTime" to now
        )
        ref.child(roomCode).updateChildren(updates)
    }

    fun updateMicMuteStatus(roomCode: String, isHost: Boolean, isMuted: Boolean) {
        val current = inMemoryRooms[roomCode]
        if (current != null) {
            val updated = if (isHost) current.copy(hostMutedMic = isMuted) else current.copy(guestMutedMic = isMuted)
            notifyLocalFlow(roomCode, updated)
        }

        val ref = roomsRef ?: return
        val field = if (isHost) "hostMutedMic" else "guestMutedMic"
        ref.child(roomCode).child(field).setValue(isMuted)
    }

    fun sendSdpOffer(roomCode: String, sdp: String) {
        val current = inMemoryRooms[roomCode]
        if (current != null) {
            val updated = current.copy(sdpOffer = sdp)
            notifyLocalFlow(roomCode, updated)
        }

        val ref = roomsRef ?: return
        ref.child(roomCode).child("sdpOffer").setValue(sdp)
    }

    fun sendSdpAnswer(roomCode: String, sdp: String) {
        val current = inMemoryRooms[roomCode]
        if (current != null) {
            val updated = current.copy(sdpAnswer = sdp)
            notifyLocalFlow(roomCode, updated)
        }

        val ref = roomsRef ?: return
        ref.child(roomCode).child("sdpAnswer").setValue(sdp)
    }

    fun sendChatMessage(roomCode: String, senderSymbol: String, text: String) {
        val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val messageData = mapOf(
            "id" to msgId,
            "sender" to senderSymbol,
            "text" to text,
            "timestamp" to System.currentTimeMillis().toString()
        )

        val current = inMemoryRooms[roomCode]
        if (current != null) {
            val updatedChat = current.chatMessages.toMutableMap()
            updatedChat[msgId] = messageData
            val updated = current.copy(chatMessages = updatedChat)
            notifyLocalFlow(roomCode, updated)
        }

        val ref = roomsRef ?: return
        ref.child(roomCode).child("chatMessages").child(msgId).setValue(messageData)
    }

    fun sendIceCandidate(roomCode: String, isHost: Boolean, candidateData: Map<String, Any>) {
        val ref = roomsRef ?: return
        val nodeName = if (isHost) "hostIceCandidates" else "guestIceCandidates"
        ref.child(roomCode).child(nodeName).push().setValue(candidateData)
    }

    fun sendAudioChunk(roomCode: String, isHost: Boolean, chunkBase64: String) {
        val ref = roomsRef ?: return
        val nodeName = if (isHost) "hostAudioChunk" else "guestAudioChunk"
        val data = mapOf(
            "data" to chunkBase64,
            "ts" to System.currentTimeMillis()
        )
        ref.child(roomCode).child(nodeName).setValue(data)
    }

    fun observeAudioChunks(
        roomCode: String,
        isHost: Boolean,
        onChunkReceived: (String) -> Unit
    ) {
        val ref = roomsRef ?: return
        val nodeName = if (isHost) "guestAudioChunk" else "hostAudioChunk"
        val childRef = ref.child(roomCode).child(nodeName)
        var lastProcessedAudioTs = 0L
        childRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val chunkMap = snapshot.value as? Map<*, *>
                    val dataStr = chunkMap?.get("data") as? String
                    val ts = (chunkMap?.get("ts") as? Number)?.toLong() ?: 0L
                    if (!dataStr.isNullOrEmpty() && ts > lastProcessedAudioTs) {
                        lastProcessedAudioTs = ts
                        onChunkReceived(dataStr)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun observeIceCandidates(
        roomCode: String,
        isHost: Boolean,
        onCandidateReceived: (Map<String, Any>) -> Unit
    ) {
        val ref = roomsRef ?: return
        val nodeName = if (isHost) "guestIceCandidates" else "hostIceCandidates"
        val childRef = ref.child(roomCode).child(nodeName)
        childRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                @Suppress("UNCHECKED_CAST")
                val value = snapshot.value as? Map<String, Any>
                if (value != null) {
                    onCandidateReceived(value)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun leaveRoom(roomCode: String, isHost: Boolean) {
        if (isHost) {
            inMemoryRooms.remove(roomCode)
            val f = inMemoryFlows.remove(roomCode)
            if (f != null) f.value = null
            val ref = roomsRef ?: return
            ref.child(roomCode).removeValue()
        } else {
            val current = inMemoryRooms[roomCode]
            if (current != null) {
                val updated = current.copy(
                    playerGuestId = null,
                    status = "WAITING"
                )
                notifyLocalFlow(roomCode, updated)
            }
            val ref = roomsRef ?: return
            val updates = mapOf<String, Any?>(
                "playerGuestId" to null,
                "status" to "WAITING"
            )
            ref.child(roomCode).updateChildren(updates)
        }
    }
}
