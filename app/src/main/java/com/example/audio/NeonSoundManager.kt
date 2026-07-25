package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

class NeonSoundManager(context: Context) {
    private val appContext: Context = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()

    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                resumeMusic()
            } else {
                pauseMusic()
            }
        }
    var isHapticsEnabled: Boolean = true

    private var bgMusicTrack: AudioTrack? = null
    private var bgMusicPcm: ByteArray? = null

    // Pre-synthesized SFX PCM buffers for zero-latency playback
    private val tapPcm by lazy { generateTapPcm() }
    private val winPcm by lazy { generateWinPcm() }
    private val losePcm by lazy { generateLosePcm() }
    private val drawPcm by lazy { generateDrawPcm() }
    private val hintPcm by lazy { generateHintPcm() }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        // Pre-generate background music in background
        executor.execute {
            try {
                bgMusicPcm = generateSynthwaveBackgroundPcm()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startMusic() {
        if (!isMusicEnabled) return
        executor.execute {
            try {
                if (bgMusicTrack == null) {
                    val pcm = bgMusicPcm ?: generateSynthwaveBackgroundPcm().also { bgMusicPcm = it }
                    val track = createAudioTrack(pcm.size)
                    track.write(pcm, 0, pcm.size)
                    track.setLoopPoints(0, pcm.size / 2, -1)
                    track.setVolume(0.35f)
                    track.play()
                    bgMusicTrack = track
                } else if (bgMusicTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    bgMusicTrack?.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseMusic() {
        executor.execute {
            try {
                if (bgMusicTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    bgMusicTrack?.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeMusic() {
        if (!isMusicEnabled) return
        executor.execute {
            try {
                if (bgMusicTrack == null) {
                    startMusic()
                } else if (bgMusicTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    bgMusicTrack?.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopMusic() {
        executor.execute {
            try {
                bgMusicTrack?.stop()
                bgMusicTrack?.release()
                bgMusicTrack = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playTap() {
        if (!isSoundEnabled) return
        playSfx(tapPcm, volume = 0.6f)
    }

    fun playWin() {
        if (!isSoundEnabled) return
        playSfx(winPcm, volume = 0.85f)
    }

    fun playLose() {
        if (!isSoundEnabled) return
        playSfx(losePcm, volume = 0.85f)
    }

    fun playDraw() {
        if (!isSoundEnabled) return
        playSfx(drawPcm, volume = 0.75f)
    }

    fun playHint() {
        if (!isSoundEnabled) return
        playSfx(hintPcm, volume = 0.7f)
    }

    private fun playSfx(pcm: ByteArray, volume: Float) {
        executor.execute {
            try {
                val track = createAudioTrack(pcm.size)
                track.write(pcm, 0, pcm.size)
                track.setVolume(volume)
                track.play()
                val durationMs = (pcm.size / 2 * 1000L) / SAMPLE_RATE
                Thread.sleep(durationMs + 100)
                track.stop()
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createAudioTrack(bufferSize: Int): AudioTrack {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        return AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    fun triggerHapticClick() {
        if (!isHapticsEnabled) return
        executor.execute {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerHapticWin() {
        if (!isHapticsEnabled) return
        executor.execute {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val timings = longArrayOf(0, 50, 50, 100)
                    val amplitudes = intArrayOf(0, 150, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        stopMusic()
        executor.shutdown()
    }

    companion object {
        private const val SAMPLE_RATE = 44100

        private fun writeShortToBytes(bytes: ByteArray, offset: Int, value: Short) {
            bytes[offset] = (value.toInt() and 0xFF).toByte()
            bytes[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
        }

        private fun generateTapPcm(): ByteArray {
            val durationMs = 45
            val totalSamples = (SAMPLE_RATE * durationMs) / 1000
            val bytes = ByteArray(totalSamples * 2)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / totalSamples
                val freq = 800.0 - (500.0 * progress)
                val envelope = (1.0 - progress) * (if (progress < 0.1) progress / 0.1 else 1.0)
                val sample = sin(2.0 * PI * freq * t) * 0.7 + (Math.abs((t * freq % 1.0) - 0.5) * 4.0 - 1.0) * 0.3
                val pcmValue = (sample * envelope * 28000).toInt().coerceIn(-32767, 32767).toShort()
                writeShortToBytes(bytes, i * 2, pcmValue)
            }
            return bytes
        }

        private fun generateHintPcm(): ByteArray {
            val durationMs = 150
            val totalSamples = (SAMPLE_RATE * durationMs) / 1000
            val bytes = ByteArray(totalSamples * 2)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / totalSamples
                val freq = if (progress < 0.4) 880.0 else 1318.5
                val localProgress = if (progress < 0.4) progress / 0.4 else (progress - 0.4) / 0.6
                val envelope = (1.0 - localProgress) * 0.9
                val sample = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)
                val pcmValue = (sample * envelope * 22000).toInt().coerceIn(-32767, 32767).toShort()
                writeShortToBytes(bytes, i * 2, pcmValue)
            }
            return bytes
        }

        private fun generateWinPcm(): ByteArray {
            val durationMs = 850
            val totalSamples = (SAMPLE_RATE * durationMs) / 1000
            val bytes = ByteArray(totalSamples * 2)
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / totalSamples

                val noteIndex = (progress * 5.0).toInt().coerceIn(0, 4)
                val isFinalChord = noteIndex == 4

                val sample = if (!isFinalChord) {
                    val freq = notes[noteIndex]
                    val noteProgress = (progress * 5.0) - noteIndex
                    val env = (1.0 - noteProgress * 0.7)
                    val sq = if (sin(2.0 * PI * freq * t) >= 0) 0.6 else -0.6
                    val sine = sin(2.0 * PI * freq * t) * 0.4
                    (sq + sine) * env
                } else {
                    val env = (1.0 - (progress - 0.8) / 0.2).coerceIn(0.0, 1.0)
                    val s1 = sin(2.0 * PI * 1046.50 * t) * 0.4
                    val s2 = sin(2.0 * PI * 1318.51 * t) * 0.4
                    val s3 = sin(2.0 * PI * 1567.98 * t) * 0.3
                    (s1 + s2 + s3) * env
                }

                val pcmValue = (sample * 24000).toInt().coerceIn(-32767, 32767).toShort()
                writeShortToBytes(bytes, i * 2, pcmValue)
            }
            return bytes
        }

        private fun generateLosePcm(): ByteArray {
            val durationMs = 900
            val totalSamples = (SAMPLE_RATE * durationMs) / 1000
            val bytes = ByteArray(totalSamples * 2)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / totalSamples

                val freq = when {
                    progress < 0.25 -> 392.0
                    progress < 0.50 -> 311.13
                    progress < 0.70 -> 261.63
                    else -> {
                        val slideProgress = (progress - 0.70) / 0.30
                        220.0 - (130.0 * slideProgress)
                    }
                }

                val envelope = (1.0 - progress * 0.8)
                val tremolo = 1.0 + 0.15 * sin(2.0 * PI * 12.0 * t)
                val sq = if (sin(2.0 * PI * freq * t) >= 0) 0.65 else -0.65
                val tri = (Math.abs((t * freq % 1.0) - 0.5) * 4.0 - 1.0) * 0.35

                val sample = (sq + tri) * envelope * tremolo
                val pcmValue = (sample * 23000).toInt().coerceIn(-32767, 32767).toShort()
                writeShortToBytes(bytes, i * 2, pcmValue)
            }
            return bytes
        }

        private fun generateDrawPcm(): ByteArray {
            val durationMs = 500
            val totalSamples = (SAMPLE_RATE * durationMs) / 1000
            val bytes = ByteArray(totalSamples * 2)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / totalSamples

                val freq = if (progress < 0.5) 329.63 else 311.13
                val envelope = (1.0 - progress) * 0.9
                val wobble = sin(2.0 * PI * 8.0 * t) * 15.0

                val sample = sin(2.0 * PI * (freq + wobble) * t) * 0.7 +
                        (if (sin(2.0 * PI * (freq * 0.5) * t) >= 0) 0.3 else -0.3)

                val pcmValue = (sample * envelope * 22000).toInt().coerceIn(-32767, 32767).toShort()
                writeShortToBytes(bytes, i * 2, pcmValue)
            }
            return bytes
        }

        private fun generateSynthwaveBackgroundPcm(): ByteArray {
            val durationMs = 6000
            val totalSamples = (SAMPLE_RATE * durationMs) / 1000
            val bytes = ByteArray(totalSamples * 2)

            val bassFreqs = doubleArrayOf(110.0, 87.31, 130.81, 98.0)
            val arpChords = arrayOf(
                doubleArrayOf(220.0, 261.63, 329.63, 440.0),
                doubleArrayOf(174.61, 220.0, 261.63, 349.23),
                doubleArrayOf(261.63, 329.63, 392.0, 523.25),
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)
            )

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / totalSamples

                val barIndex = (progress * 4.0).toInt().coerceIn(0, 3)
                val barProgress = (progress * 4.0) - barIndex

                val bassFreq = bassFreqs[barIndex]
                val bassSaw = (Math.abs((t * bassFreq % 1.0) - 0.5) * 2.0 - 0.5) * 0.35
                val bassSine = sin(2.0 * PI * bassFreq * t) * 0.45

                val sixteenthIndex = (barProgress * 16.0).toInt().coerceIn(0, 15)
                val sixteenthProgress = (barProgress * 16.0) - sixteenthIndex
                val noteInChord = arpChords[barIndex][sixteenthIndex % 4]

                val arpEnv = (1.0 - sixteenthProgress * 0.85).coerceIn(0.0, 1.0)
                val arpPulse = (if (sin(2.0 * PI * noteInChord * t) >= 0) 0.25 else -0.25) * arpEnv

                val mix = bassSaw + bassSine + arpPulse
                val pcmValue = (mix * 18000).toInt().coerceIn(-32767, 32767).toShort()
                writeShortToBytes(bytes, i * 2, pcmValue)
            }
            return bytes
        }
    }
}
