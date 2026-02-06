package com.example.cakecompiler.presentation.effect

import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.usecase.CakeCandidate
import com.example.cakecompiler.usecase.ObserveSerendipityAnomalyUseCase
import kotlin.math.sqrt

/**
 * `shakeToSerendipity()` - 隠しコマンド
 *
 * 端末を物理的にシェイクすることで、計算外の意外な選択肢を提案する。
 * `observeSerendipityAnomaly()` を強制的にトリガーし、
 * 統計データから最も遠いベクトル（Divergence最大値）のケーキを提示。
 */
class SerendipityShakeEffect(
    private val observeSerendipity: ObserveSerendipityAnomalyUseCase = ObserveSerendipityAnomalyUseCase()
) {

    /**
     * シェイクによるSerendipityモードを実行
     *
     * @param optimalPreference 計算された最適なプレファレンス
     * @param candidates 候補のケーキリスト
     * @return 最も意外な（divergence最大の）ケーキ
     */
    fun findMostDivergentCake(
        optimalPreference: PreferenceVector,
        candidates: List<CakeCandidate>
    ): SerendipityResult {
        if (candidates.isEmpty()) {
            return SerendipityResult.NoCandidates
        }

        // 各ケーキと最適プレファレンスとの距離を計算
        val divergences = candidates.map { cake ->
            cake to optimalPreference.distanceTo(cake.vector)
        }

        // 最も遠い（意外な）ケーキを選択
        val mostDivergent = divergences.maxByOrNull { it.second }
            ?: return SerendipityResult.NoCandidates

        val (cake, divergence) = mostDivergent

        // Serendipityを強制的にトリガー
        val serendipityEvent = observeSerendipity.detectSerendipity(
            expectedPreference = optimalPreference,
            actualPreference = cake.vector
        )

        return SerendipityResult.Found(
            cake = cake,
            divergenceScore = divergence,
            normalizedDivergence = (divergence / MAX_DIVERGENCE).coerceIn(0.0, 1.0),
            forcedSerendipityEvent = serendipityEvent
        )
    }

    /**
     * 確率的に意外なケーキを選ぶ（divergenceで重み付け）
     * より遠いケーキほど選ばれやすい
     */
    fun selectRandomDivergentCake(
        optimalPreference: PreferenceVector,
        candidates: List<CakeCandidate>,
        randomValue: Double = kotlin.random.Random.nextDouble()
    ): SerendipityResult {
        if (candidates.isEmpty()) {
            return SerendipityResult.NoCandidates
        }

        // divergenceで重み付け
        val weights = candidates.map { cake ->
            cake to optimalPreference.distanceTo(cake.vector)
        }

        val totalWeight = weights.sumOf { it.second }
        if (totalWeight == 0.0) {
            return SerendipityResult.NoCandidates
        }

        // 確率的に選択
        var cumulative = 0.0
        val threshold = randomValue * totalWeight

        for ((cake, weight) in weights) {
            cumulative += weight
            if (cumulative >= threshold) {
                val serendipityEvent = observeSerendipity.detectSerendipity(
                    expectedPreference = optimalPreference,
                    actualPreference = cake.vector
                )
                return SerendipityResult.Found(
                    cake = cake,
                    divergenceScore = weight,
                    normalizedDivergence = (weight / MAX_DIVERGENCE).coerceIn(0.0, 1.0),
                    forcedSerendipityEvent = serendipityEvent
                )
            }
        }

        // フォールバック
        return findMostDivergentCake(optimalPreference, candidates)
    }

    /**
     * 普段選ばれにくいケーキをフィルタ
     * 過去の選択履歴から遠いものを抽出
     */
    fun filterUnusualCakes(
        candidates: List<CakeCandidate>,
        pastChoices: List<PreferenceVector>,
        threshold: Double = 0.5
    ): List<CakeCandidate> {
        if (pastChoices.isEmpty()) return candidates

        val averagePast = averageVector(pastChoices)

        return candidates.filter { cake ->
            averagePast.distanceTo(cake.vector) >= threshold
        }
    }

    private fun averageVector(vectors: List<PreferenceVector>): PreferenceVector {
        if (vectors.isEmpty()) return PreferenceVector.neutral()

        val avgSweet = vectors.map { it.sweetness }.average()
        val avgSour = vectors.map { it.sourness }.average()
        val avgTexture = vectors.map { it.texture }.average()
        val avgTemp = vectors.map { it.temperature }.average()
        val avgArt = vectors.map { it.artistry }.average()

        return PreferenceVector(avgSweet, avgSour, avgTexture, avgTemp, avgArt)
    }

    companion object {
        // 5次元空間での最大距離
        val MAX_DIVERGENCE = sqrt(5.0)
    }
}

/**
 * Serendipity検索の結果
 */
sealed class SerendipityResult {
    /**
     * 候補なし
     */
    data object NoCandidates : SerendipityResult()

    /**
     * 意外なケーキを発見
     */
    data class Found(
        val cake: CakeCandidate,
        val divergenceScore: Double,
        val normalizedDivergence: Double,  // 0.0 ~ 1.0
        val forcedSerendipityEvent: com.example.cakecompiler.domain.model.SerendipityEvent?
    ) : SerendipityResult() {

        /**
         * どれくらい「意外」か（パーセント表示用）
         */
        val surprisePercentage: Int
            get() = (normalizedDivergence * 100).toInt()

        /**
         * 意外さのレベル
         */
        val surpriseLevel: SurpriseLevel
            get() = when {
                normalizedDivergence >= 0.7 -> SurpriseLevel.VERY_SURPRISING
                normalizedDivergence >= 0.5 -> SurpriseLevel.SURPRISING
                normalizedDivergence >= 0.3 -> SurpriseLevel.SOMEWHAT_SURPRISING
                else -> SurpriseLevel.MILD
            }
    }
}

/**
 * 意外さのレベル
 */
enum class SurpriseLevel {
    MILD,               // 少し意外
    SOMEWHAT_SURPRISING, // まあまあ意外
    SURPRISING,          // かなり意外
    VERY_SURPRISING      // とても意外
}
