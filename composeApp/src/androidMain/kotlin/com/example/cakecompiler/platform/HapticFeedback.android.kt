package com.example.cakecompiler.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Android用Hapticフィードバック実装
 */
class AndroidHapticFeedback(private val context: Context) : HapticFeedback {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun playHeartbeat() {
        // 「ト・クン」- 鼓動のような2段階の振動
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 80, 100, 120)  // 間隔, 振動, 間隔, 振動
            val amplitudes = intArrayOf(0, 200, 0, 255) // 弱め, 強め
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
        }
    }

    override fun playLightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    override fun playSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    override fun playWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 50, 30)
            val amplitudes = intArrayOf(0, 150, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 30, 50, 30), -1)
        }
    }

    override fun playShakeDetected() {
        // シェイク検出時の特徴的なフィードバック
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 30, 50, 30, 100)
            val amplitudes = intArrayOf(0, 100, 0, 150, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 30, 50, 30, 100), -1)
        }
    }

    override fun playPattern(pattern: HapticPattern) {
        when (pattern) {
            is HapticPattern.Impact -> playImpact(pattern.intensity)
            is HapticPattern.Pulse -> playPulse(pattern)
            is HapticPattern.Heartbeat -> playHeartbeat()
            is HapticPattern.Success -> playSuccess()
            is HapticPattern.Jiggle -> playJiggle(pattern.intensity)
        }
    }

    private fun playImpact(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (intensity * 255).toInt().coerceIn(1, 255)
            vibrator.vibrate(VibrationEffect.createOneShot(30, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    private fun playPulse(pulse: HapticPattern.Pulse) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = LongArray(pulse.count * 2) { index ->
                if (index % 2 == 0) 0L else pulse.intervalMs
            }
            val amplitude = (pulse.intensity * 255).toInt().coerceIn(1, 255)
            val amplitudes = IntArray(pulse.count * 2) { index ->
                if (index % 2 == 0) 0 else amplitude
            }
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pulse.intervalMs * pulse.count)
        }
    }

    private fun playJiggle(intensity: Float) {
        // 微細な振動（期待の震え）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (intensity * 100).toInt().coerceIn(1, 100)
            vibrator.vibrate(VibrationEffect.createOneShot(15, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }
}

// Context依存のため、ファクトリは呼び出し側で実装
actual fun createHapticFeedback(): HapticFeedback = NoOpHapticFeedback()
