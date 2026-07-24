package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R

class NeonSoundManager(context: Context) {
    private val appContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.applicationContext.createAttributionContext("default")
    } else {
        context.applicationContext
    }

    private var toneGenerator: ToneGenerator? = null
    private var mediaPlayer: MediaPlayer? = null

    var isSoundEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                resumeMusic()
            } else {
                pauseMusic()
            }
        }
    var isHapticsEnabled: Boolean = true

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
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startMusic() {
        if (!isSoundEnabled) return
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(appContext, R.raw.bg_synthwave)?.apply {
                    isLooping = true
                    setVolume(0.4f, 0.4f)
                }
            }
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumeMusic() {
        if (!isSoundEnabled) return
        try {
            if (mediaPlayer == null) {
                startMusic()
            } else if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playTap() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playWin() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 200)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDraw() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playHint() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerHapticClick() {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerHapticWin() {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
        stopMusic()
    }
}
