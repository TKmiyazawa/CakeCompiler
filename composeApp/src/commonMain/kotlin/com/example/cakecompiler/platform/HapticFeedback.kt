package com.example.cakecompiler.platform

/**
 * プラットフォーム固有のHapticフィードバックを抽象化
 *
 * iPhone: Taptic Engine
 * Android: Vibrator
 */
interface HapticFeedback {

    /**
     * 「ト・クン」という鼓動のようなフィードバック
     * オーバーライド時の確信を表現
     */
    fun playHeartbeat()

    /**
     * 軽いタップフィードバック
     * ケーキ選択時
     */
    fun playLightTap()

    /**
     * 成功のフィードバック
     * 決定確定時
     */
    fun playSuccess()

    /**
     * 警告のフィードバック
     * Serendipity検出時
     */
    fun playWarning()

    /**
     * シェイク検出のフィードバック
     */
    fun playShakeDetected()

    /**
     * カスタムパターンのフィードバック
     */
    fun playPattern(pattern: HapticPattern)
}

/**
 * Hapticパターンの定義
 */
sealed class HapticPattern {
    /**
     * 単一のインパクト
     */
    data class Impact(val intensity: Float) : HapticPattern() {
        init {
            require(intensity in 0f..1f) { "intensity must be in [0, 1]" }
        }
    }

    /**
     * 連続したパルス
     */
    data class Pulse(
        val count: Int,
        val intervalMs: Long,
        val intensity: Float
    ) : HapticPattern()

    /**
     * 鼓動パターン（ト・クン）
     */
    data object Heartbeat : HapticPattern()

    /**
     * 成功パターン
     */
    data object Success : HapticPattern()

    /**
     * 期待の震えパターン
     */
    data class Jiggle(val intensity: Float) : HapticPattern()
}

/**
 * expect宣言 - 各プラットフォームで実装
 */
expect fun createHapticFeedback(): HapticFeedback

/**
 * テスト用のNoOpハプティック
 */
class NoOpHapticFeedback : HapticFeedback {
    override fun playHeartbeat() {}
    override fun playLightTap() {}
    override fun playSuccess() {}
    override fun playWarning() {}
    override fun playShakeDetected() {}
    override fun playPattern(pattern: HapticPattern) {}
}
