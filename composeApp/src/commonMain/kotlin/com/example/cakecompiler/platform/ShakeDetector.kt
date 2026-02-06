package com.example.cakecompiler.platform

import kotlinx.coroutines.flow.Flow

/**
 * シェイク検出の抽象化
 *
 * `shakeToSerendipity()` 機能のための端末シェイク検出。
 */
interface ShakeDetector {

    /**
     * シェイクイベントのFlow
     */
    val shakeEvents: Flow<ShakeEvent>

    /**
     * 検出を開始
     */
    fun startDetection()

    /**
     * 検出を停止
     */
    fun stopDetection()

    /**
     * 検出感度を設定
     */
    fun setSensitivity(sensitivity: ShakeSensitivity)
}

/**
 * シェイクイベント
 */
data class ShakeEvent(
    val timestamp: Long,
    val intensity: Float,  // 0.0 ~ 1.0
    val durationMs: Long
) {
    /**
     * 強いシェイクかどうか
     */
    val isStrong: Boolean
        get() = intensity > 0.7f

    /**
     * Serendipityモードをトリガーするのに十分か
     */
    val shouldTriggerSerendipity: Boolean
        get() = intensity > 0.5f && durationMs > 300
}

/**
 * シェイク検出の感度
 */
enum class ShakeSensitivity {
    LOW,    // 強くシェイクしないと反応しない
    MEDIUM, // 通常のシェイク
    HIGH    // 軽いシェイクでも反応
}

/**
 * expect宣言 - 各プラットフォームで実装
 */
expect fun createShakeDetector(): ShakeDetector

/**
 * テスト用のモックシェイク検出器
 */
class MockShakeDetector : ShakeDetector {
    private val _shakeEvents = kotlinx.coroutines.flow.MutableSharedFlow<ShakeEvent>()
    override val shakeEvents: Flow<ShakeEvent> = _shakeEvents

    override fun startDetection() {}
    override fun stopDetection() {}
    override fun setSensitivity(sensitivity: ShakeSensitivity) {}

    suspend fun simulateShake(event: ShakeEvent) {
        _shakeEvents.emit(event)
    }
}
