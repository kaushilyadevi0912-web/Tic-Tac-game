package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.data.FirebaseRealtimeManager
import kotlinx.coroutines.*
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
    private var recordingJob: Job? = null

    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startCall(roomCode: String, isHost: Boolean) {
        currentRoomCode = roomCode
        isHostUser = isHost
        isCallActive.set(true)
        isMicMuted = false

        // 1. Send SDP Signaling
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

        val localCandidate = mapOf(
            "sdpMid" to "audio",
            "sdpMLineIndex" to 0,
            "candidate" to "candidate:1 1 UDP 2013266431 127.0.0.1 50000 typ host"
        )
        firebaseManager.sendIceCandidate(roomCode, isHost, localCandidate)

        // 3. Initialize AudioTrack for playing opponent's voice
        initAudioTrack()

        // 4. Observe audio chunks from opponent
        firebaseManager.observeAudioChunks(roomCode, isHost) { base64Chunk ->
            playAudioChunk(base64Chunk)
        }

        // 5. Start recording if mic permission is granted and mic is not muted
        if (hasMicPermission()) {
            startRecordingLoop()
        }
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initAudioTrack() {
        try {
            val minBufferSizeTrack = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val bufferSize = maxOf(minBufferSizeTrack, 4096)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfigOut)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startRecordingLoop() {
        if (recordingJob?.isActive == true) return
        recordingJob = scope.launch {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
            val bufferSize = maxOf(minBufferSize, 2048)
            val buffer = ByteArray(1024)

            try {
                if (!hasMicPermission()) return@launch

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                } else {
                    return@launch
                }

                while (isActive && isCallActive.get()) {
                    if (!isMicMuted) {
                        val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (readSize > 0) {
                            val base64Str = Base64.encodeToString(buffer, 0, readSize, Base64.NO_WRAP)
                            val room = currentRoomCode
                            if (room != null) {
                                firebaseManager.sendAudioChunk(room, isHostUser, base64Str)
                            }
                        }
                    } else {
                        delay(200)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun playAudioChunk(base64Str: String) {
        if (!isCallActive.get()) return
        scope.launch {
            try {
                val pcmBytes = Base64.decode(base64Str, Base64.NO_WRAP)
                if (pcmBytes != null && pcmBytes.isNotEmpty()) {
                    audioTrack?.write(pcmBytes, 0, pcmBytes.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleMicrophone(onStateChanged: (Boolean) -> Unit) {
        isMicMuted = !isMicMuted
        currentRoomCode?.let { room ->
            firebaseManager.updateMicMuteStatus(room, isHostUser, isMicMuted)
        }

        if (!isMicMuted) {
            if (hasMicPermission()) {
                startRecordingLoop()
            }
        }

        onStateChanged(isMicMuted)
    }

    fun stopCall() {
        isCallActive.set(false)
        isRemotePeerConnected = false
        recordingJob?.cancel()
        recordingJob = null

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
