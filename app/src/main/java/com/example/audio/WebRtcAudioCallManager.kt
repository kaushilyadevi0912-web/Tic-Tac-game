package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.data.FirebaseRealtimeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
    private var playbackJob: Job? = null

    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null

    @Volatile
    private var lastRemotePlaybackTime: Long = 0L

    private var audioChunkChannel = Channel<ByteArray>(capacity = 100)

    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startCall(roomCode: String, isHost: Boolean) {
        stopCall() // Clean up any existing active audio session

        currentRoomCode = roomCode
        isHostUser = isHost
        isCallActive.set(true)
        isMicMuted = false
        audioChunkChannel = Channel(capacity = 100)

        // Configure AudioManager for Voice Communication (Enables OS level hardware echo cancellation)
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private fun initAudioTrack() {
        try {
            audioTrack?.release()
            audioTrack = null

            val minBufferSizeTrack = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val bufferSize = if (minBufferSizeTrack > 0) maxOf(minBufferSizeTrack * 2, 4096) else 8192

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
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

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack = track
                try {
                    track.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                startPlaybackLoop()
            } else {
                track.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                for (pcmBytes in audioChunkChannel) {
                    if (!isCallActive.get()) break
                    val track = audioTrack
                    if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            try { track.play() } catch (e: Exception) { e.printStackTrace() }
                        }
                        try {
                            lastRemotePlaybackTime = System.currentTimeMillis()
                            track.write(pcmBytes, 0, pcmBytes.size)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun enableAudioEffects(sessionId: Int) {
        releaseAudioEffects()
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                acousticEchoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (AutomaticGainControl.isAvailable()) {
                automaticGainControl = AutomaticGainControl.create(sessionId)?.apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseAudioEffects() {
        try {
            acousticEchoCanceler?.enabled = false
            acousticEchoCanceler?.release()
        } catch (_: Exception) {}
        acousticEchoCanceler = null

        try {
            noiseSuppressor?.enabled = false
            noiseSuppressor?.release()
        } catch (_: Exception) {}
        noiseSuppressor = null

        try {
            automaticGainControl?.enabled = false
            automaticGainControl?.release()
        } catch (_: Exception) {}
        automaticGainControl = null
    }

    private fun createAudioRecord(bufferSize: Int): AudioRecord? {
        val sources = arrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )
        for (source in sources) {
            try {
                val recorder = AudioRecord(
                    source,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    bufferSize
                )
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    enableAudioEffects(recorder.audioSessionId)
                    return recorder
                } else {
                    recorder.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun calculateRms(buffer: ByteArray, readSize: Int): Double {
        var sum = 0.0
        var count = 0
        var i = 0
        while (i < readSize - 1) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val shortVal = sample.toShort()
            sum += shortVal * shortVal
            count++
            i += 2
        }
        if (count == 0) return 0.0
        return Math.sqrt(sum / count)
    }

    private fun startRecordingLoop() {
        if (recordingJob?.isActive == true) return
        recordingJob = scope.launch {
            try {
                if (!hasMicPermission()) return@launch

                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
                val bufferSize = if (minBufferSize > 0) maxOf(minBufferSize * 2, 4096) else 4096
                val buffer = ByteArray(1024)

                val record = createAudioRecord(bufferSize) ?: return@launch
                audioRecord = record

                try {
                    record.startRecording()
                } catch (e: Exception) {
                    e.printStackTrace()
                    try { record.release() } catch (_: Exception) {}
                    audioRecord = null
                    return@launch
                }

                while (isActive && isCallActive.get()) {
                    if (!isMicMuted) {
                        val currentRecord = audioRecord ?: break
                        if (currentRecord.state != AudioRecord.STATE_INITIALIZED) break
                        val readSize = try {
                            currentRecord.read(buffer, 0, buffer.size)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            -1
                        }
                        if (readSize > 0) {
                            val rms = calculateRms(buffer, readSize)
                            // Gentle voice activity threshold (120.0 RMS) so human voice is never cut off,
                            // while hardware AcousticEchoCanceler & MODE_IN_COMMUNICATION handle speaker feedback echo.
                            if (rms > 120.0) {
                                val base64Str = Base64.encodeToString(buffer, 0, readSize, Base64.NO_WRAP)
                                val room = currentRoomCode
                                if (room != null) {
                                    firebaseManager.sendAudioChunk(room, isHostUser, base64Str)
                                }
                            }
                        } else if (readSize < 0) {
                            delay(100)
                        }
                    } else {
                        delay(200)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                releaseAudioEffects()
                try {
                    audioRecord?.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    audioRecord?.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                audioRecord = null
            }
        }
    }

    private fun playAudioChunk(base64Str: String) {
        if (!isCallActive.get()) return
        try {
            val pcmBytes = Base64.decode(base64Str, Base64.NO_WRAP)
            if (pcmBytes != null && pcmBytes.isNotEmpty()) {
                audioChunkChannel.trySend(pcmBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

        playbackJob?.cancel()
        playbackJob = null

        // Reset AudioManager mode
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            audioChunkChannel.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scope.launch {
            releaseAudioEffects()
            try {
                audioRecord?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                audioRecord?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            audioRecord = null

            try {
                audioTrack?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                audioTrack?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            audioTrack = null
        }
    }
}

