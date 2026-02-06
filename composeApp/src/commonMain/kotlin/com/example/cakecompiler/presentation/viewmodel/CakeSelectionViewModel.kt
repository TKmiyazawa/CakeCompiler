package com.example.cakecompiler.presentation.viewmodel

import com.example.cakecompiler.domain.model.ButterflyEffect
import com.example.cakecompiler.domain.model.HappinessScore
import com.example.cakecompiler.domain.model.HappinessWeights
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.RankedCake
import com.example.cakecompiler.domain.model.UserChoice
import com.example.cakecompiler.platform.HapticFeedback
import com.example.cakecompiler.platform.ShakeDetector
import com.example.cakecompiler.presentation.effect.SerendipityResult
import com.example.cakecompiler.presentation.effect.SerendipityShakeEffect
import com.example.cakecompiler.presentation.model.CakeDisplayState
import com.example.cakecompiler.presentation.model.CakeUiEffect
import com.example.cakecompiler.presentation.model.CakeUiEvent
import com.example.cakecompiler.presentation.model.DisplayCake
import com.example.cakecompiler.presentation.model.OverrideAnimationState
import com.example.cakecompiler.presentation.model.ScreenState
import com.example.cakecompiler.presentation.model.SerendipityMode
import com.example.cakecompiler.presentation.model.StatusMessage
import com.example.cakecompiler.usecase.ApplyUserOverrideUseCase
import com.example.cakecompiler.usecase.CakeCandidate
import com.example.cakecompiler.usecase.CalculateHappinessUseCase
import com.example.cakecompiler.usecase.ObserveSerendipityAnomalyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ケーキ選択画面のViewModel
 *
 * 数理モデル（CalculateHappinessUseCase等）と遊び心のあるUI表現を橋渡しする。
 * 「二人で楽しむためのデバイス」としての振る舞いを実装。
 */
class CakeSelectionViewModel(
    private val calculateHappiness: CalculateHappinessUseCase = CalculateHappinessUseCase(),
    private val observeSerendipity: ObserveSerendipityAnomalyUseCase = ObserveSerendipityAnomalyUseCase(),
    private val applyOverride: ApplyUserOverrideUseCase = ApplyUserOverrideUseCase(),
    private val serendipityShakeEffect: SerendipityShakeEffect = SerendipityShakeEffect(),
    private val hapticFeedback: HapticFeedback? = null,
    private val shakeDetector: ShakeDetector? = null,
    private val coroutineScope: CoroutineScope? = null
) {
    private val _state = MutableStateFlow(CakeDisplayState())
    val state: StateFlow<CakeDisplayState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CakeUiEffect>()
    val effects: SharedFlow<CakeUiEffect> = _effects.asSharedFlow()

    // 内部状態
    private var selfPreference: PreferenceVector = PreferenceVector.neutral()
    private var partnerPreference: PreferenceVector = PreferenceVector.neutral()
    private var candidates: List<CakeCandidate> = emptyList()
    private var storedProbabilities: Map<String, Double> = emptyMap()
    private var storedDescriptions: Map<String, String> = emptyMap()
    private var currentRecommendation: RankedCake? = null

    /**
     * 初期化
     */
    fun initialize(
        selfPref: PreferenceVector,
        partnerPref: PreferenceVector,
        cakeCandidates: List<CakeCandidate>,
        probabilities: Map<String, Double> = emptyMap(),
        descriptions: Map<String, String> = emptyMap()
    ) {
        selfPreference = selfPref
        partnerPreference = partnerPref
        candidates = cakeCandidates
        storedProbabilities = probabilities
        storedDescriptions = descriptions

        _state.update { it.copy(screenState = ScreenState.Loading, statusMessage = StatusMessage.loading()) }

        // ケーキをランキング
        val ranking = calculateHappiness.rankCakes(selfPref, partnerPref, cakeCandidates)

        val displayCakes = ranking.rankings.map { ranked ->
            val probability = probabilities[ranked.cakeId] ?: (1.0 - ranked.rank * 0.1).coerceIn(0.1, 0.9)
            DisplayCake(
                id = ranked.cakeId,
                name = ranked.cakeName,
                description = descriptions[ranked.cakeId] ?: "",
                vector = ranked.score.cakeVector,
                happinessScore = ranked.score,
                probability = probability,
                rank = ranked.rank,
                isRecommended = ranked.rank == 1
            )
        }

        currentRecommendation = ranking.topChoice

        _state.update {
            it.copy(
                screenState = ScreenState.Ready(ranking.topChoice?.cakeId ?: ""),
                cakes = displayCakes,
                statusMessage = StatusMessage.complete()
            )
        }
    }

    /**
     * UIイベントを処理
     */
    fun onEvent(event: CakeUiEvent) {
        when (event) {
            is CakeUiEvent.CakeTapped -> handleCakeTapped(event.cakeId)
            is CakeUiEvent.CakeLongPressed -> handleCakeLongPressed(event.cakeId)
            is CakeUiEvent.CakeTouchStart -> handleTouchStart(event.cakeId)
            is CakeUiEvent.CakeTouchEnd -> handleTouchEnd()
            is CakeUiEvent.AcceptRecommendation -> handleAcceptRecommendation()
            is CakeUiEvent.ConfirmOverride -> handleConfirmOverride(event.cakeId)
            is CakeUiEvent.ShakeDetected -> handleShakeDetected()
            is CakeUiEvent.DismissSerendipity -> handleDismissSerendipity()
            is CakeUiEvent.Retry -> handleRetry()
            is CakeUiEvent.RestartSelection -> handleRestartSelection()
        }
    }

    private fun handleCakeTapped(cakeId: String) {
        val currentState = _state.value
        val recommendation = currentRecommendation ?: return

        // 推奨と同じならAccept
        if (cakeId == recommendation.cakeId) {
            handleAcceptRecommendation()
            return
        }

        // 違うケーキならオーバーライドの準備 — マイクロインタラクション
        _state.update {
            it.copy(
                selectedCakeId = cakeId,
                screenState = ScreenState.Overriding(
                    originalCakeId = recommendation.cakeId,
                    newCakeId = cakeId
                ),
                statusMessage = StatusMessage.overriding()
            )
        }

        emitEffect(CakeUiEffect.PlayHaptic(CakeUiEffect.HapticType.LIGHT_TAP))
    }

    private fun handleCakeLongPressed(cakeId: String) {
        // プレビュー表示（詳細情報）
        emitEffect(CakeUiEffect.PlayHaptic(CakeUiEffect.HapticType.LIGHT_TAP))
    }

    private fun handleTouchStart(cakeId: String) {
        // タッチフィードバック（揺れなし、将来の拡張ポイント）
    }

    private fun handleTouchEnd() {
        // タッチ解除（揺れなし）
    }

    private fun handleAcceptRecommendation() {
        val recommendation = currentRecommendation ?: return

        _state.update {
            it.copy(
                screenState = ScreenState.Completed(
                    chosenCakeId = recommendation.cakeId,
                    wasOverride = false
                ),
                statusMessage = StatusMessage.complete()
            )
        }

        emitEffect(CakeUiEffect.PlayHaptic(CakeUiEffect.HapticType.SUCCESS))
    }

    private fun handleConfirmOverride(cakeId: String) {
        val recommendation = currentRecommendation ?: return
        val chosenCake = candidates.find { it.id == cakeId } ?: return

        // オーバーライドアニメーションを開始
        _state.update {
            it.copy(
                overrideAnimation = it.overrideAnimation.startOverride(
                    originalCakeId = recommendation.cakeId,
                    originalCakeName = recommendation.cakeName
                )
            )
        }

        // Serendipityをチェック
        val optimalPreference = calculateHappiness.findOptimalCakePreference(
            selfPreference, partnerPreference
        )
        val serendipityEvent = observeSerendipity.detectSerendipity(
            optimalPreference, chosenCake.vector
        )

        // 確定フェーズへ移行（実際のアプリではアニメーション待ち）
        _state.update {
            var newState = it.copy(
                overrideAnimation = it.overrideAnimation.transitionToConfirmation(
                    chosenCakeId = cakeId,
                    chosenCakeName = chosenCake.name
                )
            )

            if (serendipityEvent != null) {
                newState = newState.copy(
                    serendipityMode = SerendipityMode.Detected(serendipityEvent)
                )
            }

            newState
        }

        // 「ト・クン」の鼓動
        emitEffect(CakeUiEffect.PlayHaptic(CakeUiEffect.HapticType.HEARTBEAT))

        // 完了
        _state.update {
            it.copy(
                screenState = ScreenState.Completed(
                    chosenCakeId = cakeId,
                    wasOverride = true
                ),
                overrideAnimation = it.overrideAnimation.complete(),
                statusMessage = StatusMessage.override()
            )
        }

        // ButterflyEffect — 隠し機能（AI上書き専用メッセージ）
        val butterflyMessage = ButterflyEffect.triggerForOverride()
        emitEffect(CakeUiEffect.ShowButterflyEffect(
            notification = butterflyMessage.notification,
            memoryMoment = butterflyMessage.memory.moment
        ))
    }

    /**
     * shakeToSerendipity() - 隠しコマンド
     */
    private fun handleShakeDetected() {
        emitEffect(CakeUiEffect.PlayHaptic(CakeUiEffect.HapticType.SHAKE_DETECTED))

        val optimalPreference = calculateHappiness.findOptimalCakePreference(
            selfPreference, partnerPreference
        )

        val result = serendipityShakeEffect.findMostDivergentCake(
            optimalPreference, candidates
        )

        when (result) {
            is SerendipityResult.Found -> {
                // 最も意外なケーキを見つけた！
                val divergentCake = _state.value.cakes.find { it.id == result.cake.id }

                _state.update {
                    it.copy(
                        serendipityMode = SerendipityMode.Active(
                            divergentCakeId = result.cake.id
                        ),
                        // 意外なケーキをハイライト
                        cakes = it.cakes.map { cake ->
                            if (cake.id == result.cake.id) {
                                cake.copy(isSerendipityPick = true)
                            } else {
                                cake
                            }
                        }
                    )
                }

                emitEffect(CakeUiEffect.ShowToast(
                    "✨ ${result.surprisePercentage}%の意外さ！"
                ))
            }
            SerendipityResult.NoCandidates -> {
                // 候補がない
            }
        }
    }

    private fun handleDismissSerendipity() {
        _state.update {
            it.copy(
                serendipityMode = SerendipityMode.Off,
                cakes = it.cakes.map { cake ->
                    cake.copy(isSerendipityPick = false)
                }
            )
        }
    }

    private fun handleRetry() {
        _state.update {
            CakeDisplayState()
        }
    }

    private fun handleRestartSelection() {
        initialize(
            selfPref = selfPreference,
            partnerPref = partnerPreference,
            cakeCandidates = candidates,
            probabilities = storedProbabilities,
            descriptions = storedDescriptions
        )
    }

    private fun emitEffect(effect: CakeUiEffect) {
        coroutineScope?.launch {
            _effects.emit(effect)
        }
    }

    /**
     * シェイク検出を開始
     */
    fun startShakeDetection() {
        shakeDetector?.startDetection()
        coroutineScope?.launch {
            shakeDetector?.shakeEvents?.collect { event ->
                if (event.shouldTriggerSerendipity) {
                    onEvent(CakeUiEvent.ShakeDetected)
                }
            }
        }
    }

    /**
     * シェイク検出を停止
     */
    fun stopShakeDetection() {
        shakeDetector?.stopDetection()
    }
}
