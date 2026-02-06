package com.example.cakecompiler.domain.model

/**
 * Represents a serendipity event - an unexpected divergence between
 * predicted and actual preferences that signals a learning opportunity.
 *
 * Serendipity is NOT an error - it's a chance to discover something new!
 *
 * @property divergenceScore The Euclidean distance between expected and actual (0 to √5)
 * @property expectedVector The predicted preference based on history
 * @property actualVector The actual preference observed
 * @property discoveredAspects Dimensions where significant divergence occurred
 * @property timestamp When the serendipity was detected
 */
data class SerendipityEvent(
    val divergenceScore: Double,
    val expectedVector: PreferenceVector,
    val actualVector: PreferenceVector,
    val discoveredAspects: List<DiscoveredAspect>,
    val timestamp: Long = currentTimeMillis()
) {
    /**
     * Returns true if this is a "strong" serendipity (divergence > 0.7)
     */
    fun isStrong(): Boolean = divergenceScore > STRONG_THRESHOLD

    /**
     * Returns true if this is a "moderate" serendipity
     */
    fun isModerate(): Boolean = divergenceScore in THRESHOLD_OF_SURPRISE..STRONG_THRESHOLD

    /**
     * Returns the most surprising dimension.
     */
    fun mostSurprisingAspect(): DiscoveredAspect? =
        discoveredAspects.maxByOrNull { it.surpriseLevel }

    companion object {
        /**
         * Threshold for detecting serendipity (Euclidean distance in 5D).
         * 0.5 is approximately 22% of the maximum possible distance (√5 ≈ 2.236).
         */
        const val THRESHOLD_OF_SURPRISE = 0.5

        /**
         * Threshold for "strong" serendipity events.
         */
        const val STRONG_THRESHOLD = 0.7

        /**
         * Maximum possible divergence in 5D unit space.
         */
        val MAX_DIVERGENCE = kotlin.math.sqrt(5.0)

        /**
         * Per-dimension threshold for identifying which aspects diverged.
         */
        const val DIMENSION_THRESHOLD = 0.3
    }
}

/**
 * Represents a discovered aspect from serendipity detection.
 * This is a learning opportunity, not an error!
 */
data class DiscoveredAspect(
    val dimension: PreferenceDimension,
    val expectedValue: Double,
    val actualValue: Double,
    val surpriseLevel: Double
) {
    /**
     * The direction of the surprise (positive = higher than expected).
     */
    val direction: SurpriseDirection
        get() = when {
            actualValue > expectedValue + 0.1 -> SurpriseDirection.HIGHER_THAN_EXPECTED
            actualValue < expectedValue - 0.1 -> SurpriseDirection.LOWER_THAN_EXPECTED
            else -> SurpriseDirection.NEUTRAL
        }

    /**
     * Human-readable description of the discovery.
     */
    fun describe(): String {
        val directionText = when (direction) {
            SurpriseDirection.HIGHER_THAN_EXPECTED -> "more"
            SurpriseDirection.LOWER_THAN_EXPECTED -> "less"
            SurpriseDirection.NEUTRAL -> "about the same"
        }
        return "Discovered preference for ${dimension.name.lowercase()}: $directionText than expected"
    }
}

/**
 * Direction of surprise in a discovered aspect.
 */
enum class SurpriseDirection {
    HIGHER_THAN_EXPECTED,
    LOWER_THAN_EXPECTED,
    NEUTRAL
}

/**
 * Platform-expect function for getting current time.
 * Implementations provided in androidMain and iosMain.
 */
expect fun currentTimeMillis(): Long
