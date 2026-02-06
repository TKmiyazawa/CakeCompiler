package com.example.cakecompiler.domain.model

/**
 * Weights for the happiness calculation formula.
 * By default, partner happiness is weighted higher (0.8) than self (0.2),
 * embodying the principle of "prioritizing partner happiness."
 *
 * Formula: H_total = ω_self * (V_self · V_cake) + ω_partner * (V_partner · V_cake)
 */
data class HappinessWeights(
    val selfWeight: Double,
    val partnerWeight: Double
) {
    init {
        require(selfWeight >= 0) { "selfWeight must be non-negative, got $selfWeight" }
        require(partnerWeight >= 0) { "partnerWeight must be non-negative, got $partnerWeight" }
        require(selfWeight + partnerWeight > 0) { "At least one weight must be positive" }
    }

    /**
     * Returns normalized weights that sum to 1.0
     */
    fun normalized(): HappinessWeights {
        val total = selfWeight + partnerWeight
        return HappinessWeights(
            selfWeight = selfWeight / total,
            partnerWeight = partnerWeight / total
        )
    }

    /**
     * Returns the ratio of partner weight to self weight.
     * Higher values indicate more partner-prioritizing behavior.
     */
    fun partnerPriorityRatio(): Double {
        return if (selfWeight == 0.0) Double.POSITIVE_INFINITY
        else partnerWeight / selfWeight
    }

    companion object {
        /**
         * Default weights prioritizing partner happiness (80%) over self (20%).
         */
        val DEFAULT = HappinessWeights(selfWeight = 0.2, partnerWeight = 0.8)

        /**
         * Equal weights for both parties.
         */
        val EQUAL = HappinessWeights(selfWeight = 0.5, partnerWeight = 0.5)

        /**
         * Self-focused weights (for comparison/testing).
         */
        val SELF_FOCUSED = HappinessWeights(selfWeight = 0.6, partnerWeight = 0.4)
    }
}
