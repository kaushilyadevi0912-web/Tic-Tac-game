package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

class FirebaseRealtimeManager(context: Context) {

    private val database: FirebaseDatabase? = try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseDatabase.getInstance()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private val roomsRef: DatabaseReference? = database?.getReference("rooms")

    val myPlayerId: String = UUID.randomUUID().toString().take(8)

    fun generate3DigitCode(): String {
        return (100..999).random().toString()
    }

    fun createRoom(
        roomCode: String,
        onSuccess: (OnlineRoomData) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = roomsRef
        if (ref == null) {
            onError("Firebase Realtime Database is not configured. Please check google-services.json.")
            return
        }

        val room = OnlineRoomData(
            roomCode = roomCode,
            status = "WAITING",
            playerHostId = myPlayerId,
            playerGuestId = null,
            activePlayer = "O",
            board = List(9) { "" },
            winner = null,
            isDraw = false
        )

        ref.child(roomCode).setValue(room)
            .addOnSuccessListener {
                onSuccess(room)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to create room in Firebase")
            }
    }

    fun joinRoom(
        roomCode: String,
        onSuccess: (OnlineRoomData) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = roomsRef
        if (ref == null) {
            onError("Firebase Realtime Database is not configured.")
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
                    onSuccess(room)
                    return
                }

                if (room.playerGuestId != null && room.playerGuestId != myPlayerId) {
                    onError("Room $roomCode is already full!")
                    return
                }

                val updatedMap = mapOf(
                    "playerGuestId" to myPlayerId,
                    "status" to "PLAYING"
                )

                roomRef.updateChildren(updatedMap)
                    .addOnSuccessListener {
                        onSuccess(room.copy(playerGuestId = myPlayerId, status = "PLAYING"))
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Failed to join room")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

    fun observeRoom(roomCode: String): Flow<OnlineRoomData?> {
        val ref = roomsRef ?: return emptyFlow()
        return callbackFlow {
            val roomRef = ref.child(roomCode)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val room = snapshot.getValue(OnlineRoomData::class.java)
                    trySend(room)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
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
        isDraw: Boolean
    ) {
        val ref = roomsRef ?: return
        val status = if (winner != null || isDraw) "FINISHED" else "PLAYING"
        val updates = mapOf(
            "board" to board,
            "activePlayer" to nextPlayer,
            "winner" to winner,
            "isDraw" to isDraw,
            "status" to status
        )
        ref.child(roomCode).updateChildren(updates)
    }

    fun updateMicMuteStatus(roomCode: String, isHost: Boolean, isMuted: Boolean) {
        val ref = roomsRef ?: return
        val field = if (isHost) "hostMutedMic" else "guestMutedMic"
        ref.child(roomCode).child(field).setValue(isMuted)
    }

    fun sendSdpOffer(roomCode: String, sdp: String) {
        val ref = roomsRef ?: return
        ref.child(roomCode).child("sdpOffer").setValue(sdp)
    }

    fun sendSdpAnswer(roomCode: String, sdp: String) {
        val ref = roomsRef ?: return
        ref.child(roomCode).child("sdpAnswer").setValue(sdp)
    }

    fun sendIceCandidate(roomCode: String, isHost: Boolean, candidateData: Map<String, Any>) {
        val ref = roomsRef ?: return
        val nodeName = if (isHost) "hostIceCandidates" else "guestIceCandidates"
        ref.child(roomCode).child(nodeName).push().setValue(candidateData)
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

    fun leaveRoom(roomCode: String) {
        val ref = roomsRef ?: return
        ref.child(roomCode).removeValue()
    }
}
