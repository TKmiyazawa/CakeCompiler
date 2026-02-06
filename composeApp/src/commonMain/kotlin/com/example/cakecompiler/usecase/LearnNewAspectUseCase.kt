package com.example.cakecompiler.usecase

import com.example.cakecompiler.domain.entity.LearningEntry
import com.example.cakecompiler.domain.entity.PartnerProfile
import com.example.cakecompiler.domain.entity.PreferenceHistory
import com.example.cakecompiler.domain.entity.PreferenceHistoryEntry
import com.example.cakecompiler.domain.entity.PreferenceSource
import com.example.cakecompiler.domain.entity.applyLearning
import com.example.cakecompiler.domain.model.DiscoveredAspect
import com.example.cakecompiler.domain.model.PreferenceDimension
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.SerendipityEvent
import com.example.cakecompiler.domain.model.currentTimeMillis

/**
 * Use case for learning new aspects from serendipity events.
 *
 * When serendipity is detected, it's an opportunity to learn and update
 * our understanding of partner preferences. This is a positive event!
 */
class LearnNewAspectUseCase(
    private val defaultLearningRate: Double = 0.3,
    private val minLearningRate: Double = 0.1,
    private val maxLearningRate: Double = 0.5
) {

    /**
     * Learns from a serendipity event and updates the partner profile.
     *
     * @param profile The current partner profile
     * @param serendipityEvent The serendipity event to learn from
     * @param learningRate How aggressively to update (0.0 to 1.0)
     * @return Updated profile with learning applied
     */
    fun learnFromSerendipity(
        profile: PartnerProfile,
        serendipityEvent: SerendipityEvent,
        learningRate: Double = defaultLearningRate
    ): LearnResult {
        val effectiveLearningRate = learningRate.coerceIn(minLearningRate, maxLearningRate)

        // Apply the learning to the profile
        val updatedProfile = profile.applyLearning(serendipityEvent, effectiveLearningRate)

        // Calculate what changed
        val dimensionChanges = calculateDimensionChanges(
            original = profile.currentPreference,
            updated = updatedProfile.currentPreference,
            aspects = serendipityEvent.discoveredAspects
        )

        return LearnResult(
            originalProfile = profile,
            updatedProfile = updatedProfile,
            serendipityEvent = serendipityEvent,
            dimensionChanges = dimensionChanges,
            learningRateUsed = effectiveLearningRate,
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Learns from an observed cake choice (without full serendipity event).
     *
     * @param profile The current partner profile
     * @param observedPreference The preference inferred from the choice
     * @param cakeId The cake that was chosen
     * @param learningRate How aggressively to update
     * @return Updated profile
     */
    fun learnFromObservation(
        profile: PartnerProfile,
        observedPreference: PreferenceVector,
        cakeId: String,
        learningRate: Double = defaultLearningRate
    ): PartnerProfile {
        val effectiveLearningRate = learningRate.coerceIn(minLearningRate, maxLearningRate)

        // Blend the observed preference with current
        val updatedPreference = profile.currentPreference.blend(
            other = observedPreference,
            selfWeight = 1.0 - effectiveLearningRate,
            otherWeight = effectiveLearningRate
        )

        return profile.copy(
            currentPreference = updatedPreference,
            preferenceHistory = profile.preferenceHistory.add(
                PreferenceHistoryEntry(
                    preference = updatedPreference,
                    source = PreferenceSource.OBSERVED_CHOICE,
                    cakeId = cakeId,
                    timestamp = currentTimeMillis()
                )
            ),
            updatedAt = currentTimeMillis()
        )
    }

    /**
     * Calculates the optimal learning rate based on confidence and surprise.
     *
     * Higher surprise + lower confidence = higher learning rate
     * Lower surprise + higher confidence = lower learning rate
     */
    fun calculateAdaptiveLearningRate(
        currentConfidence: Double,
        surpriseLevel: Double
    ): Double {
        // Base rate inversely proportional to confidence
        val confidenceFactor = 1.0 - currentConfidence

        // Surprise increases learning rate
        val surpriseFactor = surpriseLevel

        // Combine factors
        val adaptiveRate = (confidenceFactor * 0.5 + surpriseFactor * 0.5)
            .coerceIn(minLearningRate, maxLearningRate)

        return adaptiveRate
    }

    /**
     * Determines which dimensions should be updated based on discovery.
     */
    fun identifyLearningTargets(
        serendipityEvent: SerendipityEvent
    ): List<LearningTarget> {
        return serendipityEvent.discoveredAspects.map { aspect ->
            val priority = when {
                aspect.surpriseLevel > 0.5 -> LearningPriority.HIGH
                aspect.surpriseLevel > 0.3 -> LearningPriority.MEDIUM
                else -> LearningPriority.LOW
            }

            LearningTarget(
                dimension = aspect.dimension,
                currentValue = aspect.expectedValue,
                targetValue = aspect.actualValue,
                priority = priority,
                suggestedLearningRate = calculateAdaptiveLearningRate(
                    currentConfidence = 0.5,  // Default confidence
                    surpriseLevel = aspect.surpriseLevel
                )
            )
        }
    }

    private fun calculateDimensionChanges(
        original: PreferenceVector,
        updated: PreferenceVector,
        aspects: List<DiscoveredAspect>
    ): List<DimensionChange> {
        return aspects.map { aspect ->
            val originalValue = when (aspect.dimension) {
                PreferenceDimension.SWEETNESS -> original.sweetness
                PreferenceDimension.SOURNESS -> original.sourness
                PreferenceDimension.TEXTURE -> original.texture
                PreferenceDimension.TEMPERATURE -> original.temperature
                PreferenceDimension.ARTISTRY -> original.artistry
            }
            val updatedValue = when (aspect.dimension) {
                PreferenceDimension.SWEETNESS -> updated.sweetness
                PreferenceDimension.SOURNESS -> updated.sourness
                PreferenceDimension.TEXTURE -> updated.texture
                PreferenceDimension.TEMPERATURE -> updated.temperature
                PreferenceDimension.ARTISTRY -> updated.artistry
            }

            DimensionChange(
                dimension = aspect.dimension,
                previousValue = originalValue,
                newValue = updatedValue,
                changeAmount = updatedValue - originalValue
            )
        }
    }
}

/**
 * Result of learning from serendipity.
 */
data class LearnResult(
    val originalProfile: PartnerProfile,
    val updatedProfile: PartnerProfile,
    val serendipityEvent: SerendipityEvent,
    val dimensionChanges: List<DimensionChange>,
    val learningRateUsed: Double,
    val timestamp: Long
) {
    /**
     * Returns true if any dimension was significantly updated.
     */
    val hasSignificantChanges: Boolean
        get() = dimensionChanges.any { kotlin.math.abs(it.changeAmount) > 0.1 }

    /**
     * Summary of what was learned.
     */
    fun summarize(): String {
        if (dimensionChanges.isEmpty()) return "No significant changes learned."

        return dimensionChanges.joinToString("; ") { change ->
            val direction = if (change.changeAmount > 0) "increased" else "decreased"
            "${change.dimension.name.lowercase()} $direction by ${"%.2f".format(kotlin.math.abs(change.changeAmount))}"
        }
    }
}

/**
 * Records a change in a specific dimension.
 */
data class DimensionChange(
    val dimension: PreferenceDimension,
    val previousValue: Double,
    val newValue: Double,
    val changeAmount: Double
)

/**
 * A target for learning with priority.
 */
data class LearningTarget(
    val dimension: PreferenceDimension,
    val currentValue: Double,
    val targetValue: Double,
    val priority: LearningPriority,
    val suggestedLearningRate: Double
)

/**
 * Priority levels for learning updates.
 */
enum class LearningPriority {
    LOW,
    MEDIUM,
    HIGH
}
