package com.example.cakecompiler.usecase

import com.example.cakecompiler.domain.model.CakeRanking
import com.example.cakecompiler.domain.model.HappinessScore
import com.example.cakecompiler.domain.model.HappinessWeights
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.RankedCake

/**
 * Use case for calculating happiness scores for cake selections.
 *
 * Formula: H_total = ω_self * (V_self · V_cake) + ω_partner * (V_partner · V_cake)
 *
 * By default, partner happiness is weighted higher (0.8 vs 0.2),
 * embodying the principle of "prioritizing partner happiness."
 */
class CalculateHappinessUseCase {

    /**
     * Calculates the happiness score for a single cake.
     *
     * @param selfPreference The user's preference vector
     * @param partnerPreference The partner's preference vector
     * @param cakeVector The cake's characteristic vector
     * @param weights The weights for self vs partner happiness (default: partner prioritized)
     * @return The calculated happiness score with breakdown
     */
    fun calculateScore(
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector,
        cakeVector: PreferenceVector,
        weights: HappinessWeights = HappinessWeights.DEFAULT
    ): HappinessScore {
        return HappinessScore.calculate(
            selfPreference = selfPreference,
            partnerPreference = partnerPreference,
            cakeVector = cakeVector,
            weights = weights
        )
    }

    /**
     * Finds the optimal cake preference vector that maximizes total happiness.
     * This is the weighted blend of self and partner preferences.
     *
     * @param selfPreference The user's preference vector
     * @param partnerPreference The partner's preference vector
     * @param weights The weights for the blend (default: partner prioritized)
     * @return The optimal preference vector
     */
    fun findOptimalCakePreference(
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector,
        weights: HappinessWeights = HappinessWeights.DEFAULT
    ): PreferenceVector {
        val normalizedWeights = weights.normalized()
        return selfPreference.blend(
            other = partnerPreference,
            selfWeight = normalizedWeights.selfWeight,
            otherWeight = normalizedWeights.partnerWeight
        )
    }

    /**
     * Ranks a list of cakes by their happiness score.
     *
     * @param selfPreference The user's preference vector
     * @param partnerPreference The partner's preference vector
     * @param cakes The list of cakes to rank (id, name, vector)
     * @param weights The weights for scoring (default: partner prioritized)
     * @return Ranked list of cakes from highest to lowest score
     */
    fun rankCakes(
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector,
        cakes: List<CakeCandidate>,
        weights: HappinessWeights = HappinessWeights.DEFAULT
    ): CakeRanking {
        val scoredCakes = cakes.map { cake ->
            val score = calculateScore(
                selfPreference = selfPreference,
                partnerPreference = partnerPreference,
                cakeVector = cake.vector,
                weights = weights
            )
            cake to score
        }

        val sortedCakes = scoredCakes.sortedByDescending { it.second.totalScore }

        val rankedCakes = sortedCakes.mapIndexed { index, (cake, score) ->
            RankedCake(
                rank = index + 1,
                cakeId = cake.id,
                cakeName = cake.name,
                score = score
            )
        }

        return CakeRanking(rankings = rankedCakes)
    }

    /**
     * Calculates the theoretical maximum happiness score.
     * This occurs when the cake perfectly matches the optimal preference.
     *
     * @param selfPreference The user's preference vector
     * @param partnerPreference The partner's preference vector
     * @param weights The weights for calculation
     * @return The maximum possible happiness score
     */
    fun calculateMaxPossibleScore(
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector,
        weights: HappinessWeights = HappinessWeights.DEFAULT
    ): Double {
        val optimalCake = findOptimalCakePreference(selfPreference, partnerPreference, weights)
        val score = calculateScore(selfPreference, partnerPreference, optimalCake, weights)
        return score.totalScore
    }

    /**
     * Evaluates how close a cake is to the optimal choice.
     *
     * @return A value between 0.0 (worst) and 1.0 (optimal)
     */
    fun evaluateOptimality(
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector,
        cakeVector: PreferenceVector,
        weights: HappinessWeights = HappinessWeights.DEFAULT
    ): Double {
        val actualScore = calculateScore(selfPreference, partnerPreference, cakeVector, weights).totalScore
        val maxScore = calculateMaxPossibleScore(selfPreference, partnerPreference, weights)

        return if (maxScore == 0.0) 0.0 else actualScore / maxScore
    }
}

/**
 * Represents a cake candidate for ranking.
 */
data class CakeCandidate(
    val id: String,
    val name: String,
    val vector: PreferenceVector
)
