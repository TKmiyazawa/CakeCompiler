package com.example.cakecompiler.domain.model

/**
 * Result of a happiness calculation with detailed breakdown.
 *
 * @property totalScore The combined weighted happiness score
 * @property selfAlignment How well the cake matches self preferences (dot product)
 * @property partnerAlignment How well the cake matches partner preferences (dot product)
 * @property weightsUsed The weights used in the calculation
 * @property cakeVector The cake preference vector that was evaluated
 */
data class HappinessScore(
    val totalScore: Double,
    val selfAlignment: Double,
    val partnerAlignment: Double,
    val weightsUsed: HappinessWeights,
    val cakeVector: PreferenceVector
) {
    /**
     * Weighted contribution from self preferences.
     */
    val selfContribution: Double
        get() = weightsUsed.normalized().selfWeight * selfAlignment

    /**
     * Weighted contribution from partner preferences.
     */
    val partnerContribution: Double
        get() = weightsUsed.normalized().partnerWeight * partnerAlignment

    /**
     * Returns true if partner would be happier with this cake than self.
     */
    fun isPartnerFavored(): Boolean = partnerAlignment > selfAlignment

    /**
     * Returns the difference in alignment (positive = self prefers more).
     */
    fun alignmentDifference(): Double = selfAlignment - partnerAlignment

    companion object {
        /**
         * Creates a HappinessScore by calculating from preferences.
         */
        fun calculate(
            selfPreference: PreferenceVector,
            partnerPreference: PreferenceVector,
            cakeVector: PreferenceVector,
            weights: HappinessWeights = HappinessWeights.DEFAULT
        ): HappinessScore {
            val normalizedWeights = weights.normalized()
            val selfAlignment = selfPreference.dot(cakeVector)
            val partnerAlignment = partnerPreference.dot(cakeVector)
            val totalScore = normalizedWeights.selfWeight * selfAlignment +
                    normalizedWeights.partnerWeight * partnerAlignment

            return HappinessScore(
                totalScore = totalScore,
                selfAlignment = selfAlignment,
                partnerAlignment = partnerAlignment,
                weightsUsed = weights,
                cakeVector = cakeVector
            )
        }
    }
}

/**
 * A ranked list of cakes with their happiness scores.
 */
data class CakeRanking(
    val rankings: List<RankedCake>
) {
    val topChoice: RankedCake?
        get() = rankings.firstOrNull()

    val isEmpty: Boolean
        get() = rankings.isEmpty()
}

/**
 * A cake with its rank and happiness score.
 */
data class RankedCake(
    val rank: Int,
    val cakeId: String,
    val cakeName: String,
    val score: HappinessScore
)
