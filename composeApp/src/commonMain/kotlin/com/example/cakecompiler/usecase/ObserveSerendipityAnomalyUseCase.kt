package com.example.cakecompiler.usecase

import com.example.cakecompiler.domain.model.DiscoveredAspect
import com.example.cakecompiler.domain.model.PreferenceDimension
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.SerendipityEvent
import com.example.cakecompiler.domain.model.get
import kotlin.math.abs

/**
 * Use case for detecting serendipity anomalies.
 *
 * Serendipity is NOT an error! It's an opportunity for discovery and learning.
 * When the actual preference diverges significantly from expectations,
 * we've discovered something new about the partner's preferences.
 */
class ObserveSerendipityAnomalyUseCase {

    /**
     * Checks if a serendipity event has occurred based on divergence.
     *
     * @param expectedPreference The predicted preference based on history
     * @param actualPreference The observed actual preference
     * @return SerendipityEvent if divergence exceeds threshold, null otherwise
     */
    fun detectSerendipity(
        expectedPreference: PreferenceVector,
        actualPreference: PreferenceVector
    ): SerendipityEvent? {
        val divergence = expectedPreference.distanceTo(actualPreference)

        if (divergence < SerendipityEvent.THRESHOLD_OF_SURPRISE) {
            return null
        }

        val discoveredAspects = identifyDivergedDimensions(expectedPreference, actualPreference)

        return SerendipityEvent(
            divergenceScore = divergence,
            expectedVector = expectedPreference,
            actualVector = actualPreference,
            discoveredAspects = discoveredAspects
        )
    }

    /**
     * Analyzes a serendipity event to provide insights.
     *
     * @param event The serendipity event to analyze
     * @return Analysis with recommendations
     */
    fun analyzeSerendipity(event: SerendipityEvent): SerendipityAnalysis {
        val significanceLevel = when {
            event.divergenceScore > SerendipityEvent.STRONG_THRESHOLD -> SignificanceLevel.HIGH
            event.divergenceScore > SerendipityEvent.THRESHOLD_OF_SURPRISE -> SignificanceLevel.MODERATE
            else -> SignificanceLevel.LOW
        }

        val insights = event.discoveredAspects.map { aspect ->
            AspectInsight(
                dimension = aspect.dimension,
                insight = generateInsight(aspect),
                suggestedAction = suggestAction(aspect)
            )
        }

        val overallRecommendation = when {
            significanceLevel == SignificanceLevel.HIGH ->
                "Major preference shift detected! Consider updating the preference model significantly."
            insights.any { it.dimension == PreferenceDimension.SWEETNESS } ->
                "Sweetness preference has changed. This is a core taste preference worth tracking."
            else ->
                "Preference evolution detected. Learning from this discovery will improve future recommendations."
        }

        return SerendipityAnalysis(
            event = event,
            significanceLevel = significanceLevel,
            insights = insights,
            overallRecommendation = overallRecommendation,
            shouldTriggerLearning = true  // Serendipity always triggers learning
        )
    }

    /**
     * Calculates the divergence between expected and actual preferences.
     * Useful for monitoring without full serendipity detection.
     *
     * @return Divergence score (0 to √5)
     */
    fun calculateDivergence(
        expectedPreference: PreferenceVector,
        actualPreference: PreferenceVector
    ): Double = expectedPreference.distanceTo(actualPreference)

    /**
     * Checks if the divergence is approaching the threshold.
     * Useful for early warning.
     *
     * @return Warning level (0.0 = no concern, 1.0 = at threshold)
     */
    fun getDivergenceWarningLevel(
        expectedPreference: PreferenceVector,
        actualPreference: PreferenceVector
    ): Double {
        val divergence = calculateDivergence(expectedPreference, actualPreference)
        return (divergence / SerendipityEvent.THRESHOLD_OF_SURPRISE).coerceIn(0.0, 1.0)
    }

    /**
     * Identifies which dimensions have diverged significantly.
     */
    private fun identifyDivergedDimensions(
        expected: PreferenceVector,
        actual: PreferenceVector
    ): List<DiscoveredAspect> {
        return PreferenceVector.DIMENSIONS.mapNotNull { dimension ->
            val expectedValue = expected.get(dimension)
            val actualValue = actual.get(dimension)
            val difference = abs(actualValue - expectedValue)

            if (difference >= SerendipityEvent.DIMENSION_THRESHOLD) {
                DiscoveredAspect(
                    dimension = dimension,
                    expectedValue = expectedValue,
                    actualValue = actualValue,
                    surpriseLevel = difference / 1.0  // Normalize to [0, 1]
                )
            } else {
                null
            }
        }
    }

    /**
     * Generates an insight string for a discovered aspect.
     */
    private fun generateInsight(aspect: DiscoveredAspect): String {
        val dimensionName = aspect.dimension.name.lowercase().replaceFirstChar { it.uppercase() }
        val direction = when {
            aspect.actualValue > aspect.expectedValue -> "higher"
            aspect.actualValue < aspect.expectedValue -> "lower"
            else -> "different"
        }
        return "$dimensionName preference is $direction than expected " +
                "(expected: ${"%.2f".format(aspect.expectedValue)}, " +
                "actual: ${"%.2f".format(aspect.actualValue)})"
    }

    /**
     * Suggests an action based on the discovered aspect.
     */
    private fun suggestAction(aspect: DiscoveredAspect): SuggestedAction {
        return when {
            aspect.surpriseLevel > 0.5 -> SuggestedAction.UPDATE_SIGNIFICANTLY
            aspect.surpriseLevel > 0.3 -> SuggestedAction.UPDATE_MODERATELY
            else -> SuggestedAction.UPDATE_SLIGHTLY
        }
    }
}

/**
 * Analysis result from a serendipity event.
 */
data class SerendipityAnalysis(
    val event: SerendipityEvent,
    val significanceLevel: SignificanceLevel,
    val insights: List<AspectInsight>,
    val overallRecommendation: String,
    val shouldTriggerLearning: Boolean
)

/**
 * Significance level of a serendipity event.
 */
enum class SignificanceLevel {
    LOW,
    MODERATE,
    HIGH
}

/**
 * Insight for a specific aspect that diverged.
 */
data class AspectInsight(
    val dimension: PreferenceDimension,
    val insight: String,
    val suggestedAction: SuggestedAction
)

/**
 * Suggested action based on the divergence.
 */
enum class SuggestedAction {
    UPDATE_SLIGHTLY,
    UPDATE_MODERATELY,
    UPDATE_SIGNIFICANTLY
}
