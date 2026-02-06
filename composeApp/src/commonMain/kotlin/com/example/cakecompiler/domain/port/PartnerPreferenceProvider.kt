package com.example.cakecompiler.domain.port

import com.example.cakecompiler.domain.model.PreferenceDimension
import com.example.cakecompiler.domain.model.PreferenceVector

/**
 * Port interface for external preference inference (e.g., Gemini API).
 * This abstraction allows mocking for tests and swapping implementations.
 */
interface PartnerPreferenceProvider {
    /**
     * Infers partner preferences based on context.
     *
     * @param context Contextual information for inference
     * @return Inferred preference vector with confidence
     */
    suspend fun inferPreference(context: PreferenceContext): PreferenceInferenceResult

    /**
     * Gets probability distributions for each preference dimension.
     *
     * @param context Contextual information for inference
     * @return Probability distribution per dimension
     */
    suspend fun getPreferenceProbabilities(context: PreferenceContext): PreferenceProbabilityDistribution
}

/**
 * Context information for preference inference.
 */
data class PreferenceContext(
    val partnerId: String,
    val partnerName: String,
    val previousChoices: List<PreviousChoice> = emptyList(),
    val occasion: String? = null,
    val timeOfDay: TimeOfDay = TimeOfDay.AFTERNOON,
    val season: Season = Season.SPRING,
    val additionalContext: Map<String, String> = emptyMap()
)

/**
 * A previous cake choice for context.
 */
data class PreviousChoice(
    val cakeId: String,
    val cakeName: String,
    val cakeVector: PreferenceVector,
    val wasEnjoyed: Boolean,
    val rating: Int? = null  // 1-5
)

/**
 * Time of day for contextual inference.
 */
enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

/**
 * Season for contextual inference.
 */
enum class Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER
}

/**
 * Result of preference inference.
 */
data class PreferenceInferenceResult(
    val inferredPreference: PreferenceVector,
    val confidence: Double,
    val reasoning: String? = null,
    val dimensionConfidences: Map<PreferenceDimension, Double> = emptyMap()
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be in [0.0, 1.0]" }
    }

    /**
     * Returns true if the inference has high confidence (>= 0.7).
     */
    fun isHighConfidence(): Boolean = confidence >= 0.7

    /**
     * Returns true if the inference has low confidence (< 0.4).
     */
    fun isLowConfidence(): Boolean = confidence < 0.4
}

/**
 * Probability distribution for preference dimensions.
 * Each dimension has a distribution of likely values.
 */
data class PreferenceProbabilityDistribution(
    val distributions: Map<PreferenceDimension, DimensionProbability>
) {
    /**
     * Returns the most likely value for each dimension.
     */
    fun mostLikelyVector(): PreferenceVector {
        val sweetness = distributions[PreferenceDimension.SWEETNESS]?.mode ?: 0.5
        val sourness = distributions[PreferenceDimension.SOURNESS]?.mode ?: 0.5
        val texture = distributions[PreferenceDimension.TEXTURE]?.mode ?: 0.5
        val temperature = distributions[PreferenceDimension.TEMPERATURE]?.mode ?: 0.5
        val artistry = distributions[PreferenceDimension.ARTISTRY]?.mode ?: 0.5

        return PreferenceVector(sweetness, sourness, texture, temperature, artistry)
    }

    /**
     * Returns the expected value vector.
     */
    fun expectedVector(): PreferenceVector {
        val sweetness = distributions[PreferenceDimension.SWEETNESS]?.mean ?: 0.5
        val sourness = distributions[PreferenceDimension.SOURNESS]?.mean ?: 0.5
        val texture = distributions[PreferenceDimension.TEXTURE]?.mean ?: 0.5
        val temperature = distributions[PreferenceDimension.TEMPERATURE]?.mean ?: 0.5
        val artistry = distributions[PreferenceDimension.ARTISTRY]?.mean ?: 0.5

        return PreferenceVector(sweetness, sourness, texture, temperature, artistry)
    }
}

/**
 * Probability distribution for a single dimension.
 */
data class DimensionProbability(
    val mean: Double,
    val variance: Double,
    val mode: Double,
    val confidenceInterval: ClosedFloatingPointRange<Double>
) {
    init {
        require(mean in 0.0..1.0) { "mean must be in [0.0, 1.0]" }
        require(variance >= 0) { "variance must be non-negative" }
        require(mode in 0.0..1.0) { "mode must be in [0.0, 1.0]" }
    }

    /**
     * Standard deviation.
     */
    val standardDeviation: Double
        get() = kotlin.math.sqrt(variance)

    /**
     * Returns true if there's high uncertainty in this dimension.
     */
    fun isHighUncertainty(): Boolean = variance > 0.1
}

/**
 * A mock implementation for testing purposes.
 */
class MockPartnerPreferenceProvider(
    private val defaultPreference: PreferenceVector = PreferenceVector.neutral(),
    private val defaultConfidence: Double = 0.7
) : PartnerPreferenceProvider {

    private val customResponses = mutableMapOf<String, PreferenceInferenceResult>()

    fun setResponseFor(partnerId: String, result: PreferenceInferenceResult) {
        customResponses[partnerId] = result
    }

    override suspend fun inferPreference(context: PreferenceContext): PreferenceInferenceResult {
        return customResponses[context.partnerId] ?: PreferenceInferenceResult(
            inferredPreference = defaultPreference,
            confidence = defaultConfidence,
            reasoning = "Mock inference for ${context.partnerName}"
        )
    }

    override suspend fun getPreferenceProbabilities(context: PreferenceContext): PreferenceProbabilityDistribution {
        val defaultDistribution = DimensionProbability(
            mean = 0.5,
            variance = 0.05,
            mode = 0.5,
            confidenceInterval = 0.3..0.7
        )

        return PreferenceProbabilityDistribution(
            distributions = PreferenceDimension.entries.associateWith { defaultDistribution }
        )
    }
}
