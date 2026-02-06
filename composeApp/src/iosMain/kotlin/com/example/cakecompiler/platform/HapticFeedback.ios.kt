package com.example.cakecompiler.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

/**
 * iOS用Hapticフィードバック実装
 * Taptic Engineを使用
 */
class IOSHapticFeedback : HapticFeedback {

    private val impactLight = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val impactMedium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val impactHeavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notification = UINotificationFeedbackGenerator()
    private val selection = UISelectionFeedbackGenerator()

    override fun playHeartbeat() {
        // 「ト・クン」- 2段階のインパクト
        impactLight.prepare()
        impactHeavy.prepare()

        impactLight.impactOccurred()
        // 少し遅延して重いインパクト（実際のアプリではdispatch_afterを使用）
        impactHeavy.impactOccurred()
    }

    override fun playLightTap() {
        selection.prepare()
        selection.selectionChanged()
    }

    override fun playSuccess() {
        notification.prepare()
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    override fun playWarning() {
        notification.prepare()
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
    }

    override fun playShakeDetected() {
        // シェイク検出時の特徴的なフィードバック
        impactMedium.prepare()
        impactHeavy.prepare()

        impactMedium.impactOccurred()
        impactHeavy.impactOccurred()
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
        when {
            intensity < 0.33f -> {
                impactLight.prepare()
                impactLight.impactOccurred()
            }
            intensity < 0.66f -> {
                impactMedium.prepare()
                impactMedium.impactOccurred()
            }
            else -> {
                impactHeavy.prepare()
                impactHeavy.impactOccurred()
            }
        }
    }

    private fun playPulse(pulse: HapticPattern.Pulse) {
        // iOSでは連続パルスは簡易実装
        val generator = when {
            pulse.intensity < 0.5f -> impactLight
            else -> impactMedium
        }
        generator.prepare()
        repeat(pulse.count) {
            generator.impactOccurred()
        }
    }

    private fun playJiggle(intensity: Float) {
        // 微細な振動（期待の震え）
        impactLight.prepare()
        impactLight.impactOccurred()
    }
}

actual fun createHapticFeedback(): HapticFeedback = IOSHapticFeedback()
