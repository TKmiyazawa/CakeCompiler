package com.example.cakecompiler.presentation.model

import com.example.cakecompiler.domain.model.HappinessScore
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.SerendipityEvent

/**
 * ケーキ選択画面のUI状態
 *
 * 数理モデルの整合性を保ちながら、遊び心のある表示を実現。
 */
data class CakeDisplayState(
    val screenState: ScreenState = ScreenState.Initial,
    val cakes: List<DisplayCake> = emptyList(),
    val overrideAnimation: OverrideAnimationState = OverrideAnimationState(),
    val statusMessage: StatusMessage? = null,
    val serendipityMode: SerendipityMode = SerendipityMode.Off,
    val selectedCakeId: String? = null
)

/**
 * 画面の状態
 */
sealed class ScreenState {
    data object Initial : ScreenState()
    data object Loading : ScreenState()
    data class Ready(val recommendedCakeId: String) : ScreenState()
    data class Overriding(val originalCakeId: String, val newCakeId: String) : ScreenState()
    data class Completed(val chosenCakeId: String, val wasOverride: Boolean) : ScreenState()
    data class Error(val message: String) : ScreenState()
}

/**
 * 表示用のケーキデータ
 */
data class DisplayCake(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val vector: PreferenceVector,
    val happinessScore: HappinessScore,
    val probability: Double,  // Geminiからの期待確率
    val rank: Int,
    val isRecommended: Boolean = false,
    val isSerendipityPick: Boolean = false
) {
    /**
     * 幸福度に応じた表示メッセージ
     */
    val happinessMessage: String
        get() = WhimsicalStrings.HappinessLevel.getMessage(happinessScore.totalScore)

    /**
     * パートナーの方が喜ぶかどうか
     */
    val partnerWouldLoveIt: Boolean
        get() = happinessScore.isPartnerFavored()
}

/**
 * Serendipityモードの状態
 */
sealed class SerendipityMode {
    /**
     * 通常モード
     */
    data object Off : SerendipityMode()

    /**
     * シェイクでSerendipityモードが有効化中
     */
    data class Activating(
        val progress: Float  // 0.0 -> 1.0
    ) : SerendipityMode()

    /**
     * Serendipityモード有効
     */
    data class Active(
        val divergentCakeId: String,
        val message: String = WhimsicalStrings.ShakeToSerendipity.randomSuggestion()
    ) : SerendipityMode()

    /**
     * Serendipity検出された
     */
    data class Detected(
        val event: SerendipityEvent,
        val message: String = WhimsicalStrings.SerendipityDetected.random()
    ) : SerendipityMode()
}

/**
 * UIイベント
 */
sealed class CakeUiEvent {
    /**
     * ケーキがタップされた
     */
    data class CakeTapped(val cakeId: String) : CakeUiEvent()

    /**
     * ケーキが長押しされた（プレビュー）
     */
    data class CakeLongPressed(val cakeId: String) : CakeUiEvent()

    /**
     * ケーキへのタッチ開始
     */
    data class CakeTouchStart(val cakeId: String) : CakeUiEvent()

    /**
     * ケーキへのタッチ終了
     */
    data object CakeTouchEnd : CakeUiEvent()

    /**
     * 推奨を受け入れ
     */
    data object AcceptRecommendation : CakeUiEvent()

    /**
     * オーバーライドを決定
     */
    data class ConfirmOverride(val cakeId: String) : CakeUiEvent()

    /**
     * シェイクが検出された
     */
    data object ShakeDetected : CakeUiEvent()

    /**
     * Serendipityモードを終了
     */
    data object DismissSerendipity : CakeUiEvent()

    /**
     * リトライ
     */
    data object Retry : CakeUiEvent()

    /**
     * 最初に戻る（選択をリセットして再演算）
     */
    data object RestartSelection : CakeUiEvent()
}

/**
 * UIへのエフェクト（一度だけ実行）
 */
sealed class CakeUiEffect {
    /**
     * Hapticを再生
     */
    data class PlayHaptic(val type: HapticType) : CakeUiEffect()

    /**
     * トースト表示
     */
    data class ShowToast(val message: String) : CakeUiEffect()

    /**
     * 画面遷移
     */
    data class Navigate(val destination: String) : CakeUiEffect()

    /**
     * ButterflyEffect — 隠し機能のメッセージ表示
     */
    data class ShowButterflyEffect(
        val notification: String,
        val memoryMoment: String
    ) : CakeUiEffect()

    enum class HapticType {
        HEARTBEAT,
        LIGHT_TAP,
        SUCCESS,
        WARNING,
        SHAKE_DETECTED
    }
}
