package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.example.data.FirebaseRealtimeManager
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class WebRtcAudioCallManager(
    private val context: Context,
    private val firebaseManager: FirebaseRealtimeManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isMicMuted: Boolean = false
        private set

    var isRemotePeerConnected: Boolean = false
        private set

    private var isCallActive = AtomicBoolean(false)
    private var currentRoomCode: String? = null
    private var isHostUser: Boolean = false

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    fun startCall(roomCode: String, isHost: Boolean) {
        currentRoomCode = roomCode
        isHostUser = isHost
        isCallActive.set(true)

        // 1. Send SDP Signaling (Offer/Answer simulation for Firebase P2P Audio link)
        if (isHost) {
            val offerSdp = "v=0\r\no=- 123456 2 IN IP4 127.0.0.1\r\ns=TicTacToe Voice Chat\r\nt=0 0\r\nm=audio 50000 RTP/AVP 0\r\nc=IN IP4 127.0.0.1"
            firebaseManager.sendSdpOffer(roomCode, offerSdp)
        } else {
            val answerSdp = "v=0\r\no=- 654321 2 IN IP4 127.0.0.1\r\ns=TicTacToe Voice Chat\r\nt=0 0\r\nm=audio 50000 RTP/AVP 0\r\nc=IN IP4 127.0.0.1"
            firebaseManager.sendSdpAnswer(roomCode, answerSdp)
        }

        // 2. Register ICE Candidate listener via Firebase
        firebaseManager.observeIceCandidates(roomCode, isHost) { candidate ->
            isRemotePeerConnected = true
        }

        // Send local ICE candidate
        val localCandidate = mapOf(
            "sdpMid" to "audio",
            "sdpMLineIndex" to 0,
            "candidate" to "candidate:1 1 UDP 2013266431 127.0.0.1 50000 typ host"
        )
        firebaseManager.sendIceCandidate(roomCode, isHost, localCandidate)
    }

    fun toggleMicrophone(onStateChanged: (Boolean) -> Unit) {
        isMicMuted = !isMicMuted
        currentRoomCode?.let { room ->
            firebaseManager.updateMicMuteStatus(room, isHostUser, isMicMuted)
        }
        onStateChanged(isMicMuted)
    }

    fun stopCall() {
        isCallActive.set(false)
        isRemotePeerConnected = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
