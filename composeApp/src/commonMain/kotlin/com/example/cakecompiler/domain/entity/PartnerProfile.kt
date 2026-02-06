package com.example.cakecompiler.domain.entity

import com.example.cakecompiler.domain.model.DiscoveredAspect
import com.example.cakecompiler.domain.model.PreferenceDimension
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.SerendipityEvent
import com.example.cakecompiler.domain.model.currentTimeMillis

/**
 * Represents a partner's profile with their preference history and learned preferences.
 * This entity evolves over time as we learn more about the partner.
 */
data class PartnerProfile(
    val id: String,
    val name: String,
    val currentPreference: PreferenceVector,
    val preferenceHistory: PreferenceHistory,
    val learningEntries: List<LearningEntry> = emptyList(),
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
) {
    /**
     * Returns the confidence level for each preference dimension based on history.
     */
    fun confidencePerDimension(): Map<PreferenceDimension, Double> {
        val historySize = preferenceHistory.entries.size
        if (historySize == 0) return PreferenceDimension.entries.associateWith { 0.0 }

        // Base confidence from history size (max at 10 entries)
        val baseConfidence = (historySize.coerceAtMost(10) / 10.0) * 0.7

        // Additional confidence from serendipity learnings
        return PreferenceDimension.entries.associateWith { dimension ->
            val learningBoost = learningEntries
                .filter { it.dimension == dimension }
                .sumOf { it.confidenceGain }
                .coerceAtMost(0.3)
            (baseConfidence + learningBoost).coerceAtMost(1.0)
        }
    }

    /**
     * Returns the overall confidence in the partner's preferences.
     */
    fun overallConfidence(): Double =
        confidencePerDimension().values.average()

    /**
     * Returns how many times the preference has been updated.
     */
    val updateCount: Int
        get() = preferenceHistory.entries.size

    /**
     * Returns the most recent serendipity event if any.
     */
    fun mostRecentLearning(): LearningEntry? =
        learningEntries.maxByOrNull { it.timestamp }

    companion object {
        /**
         * Creates a new partner profile with initial preferences.
         */
        fun create(
            id: String,
            name: String,
            initialPreference: PreferenceVector = PreferenceVector.neutral()
        ): PartnerProfile = PartnerProfile(
            id = id,
            name = name,
            currentPreference = initialPreference,
            preferenceHistory = PreferenceHistory(
                entries = listOf(
                    PreferenceHistoryEntry(
                        preference = initialPreference,
                        source = PreferenceSource.INITIAL,
                        timestamp = currentTimeMillis()
                    )
                )
            )
        )
    }
}

/**
 * Tracks the history of preference observations for a partner.
 */
data class PreferenceHistory(
    val entries: List<PreferenceHistoryEntry>
) {
    /**
     * Returns the average preference vector across all history.
     */
    fun averagePreference(): PreferenceVector? {
        if (entries.isEmpty()) return null

        val avgSweet = entries.map { it.preference.sweetness }.average()
        val avgSour = entries.map { it.preference.sourness }.average()
        val avgTexture = entries.map { it.preference.texture }.average()
        val avgTemp = entries.map { it.preference.temperature }.average()
        val avgArt = entries.map { it.preference.artistry }.average()

        return PreferenceVector(avgSweet, avgSour, avgTexture, avgTemp, avgArt)
    }

    /**
     * Returns entries from a specific source.
     */
    fun entriesFromSource(source: PreferenceSource): List<PreferenceHistoryEntry> =
        entries.filter { it.source == source }

    /**
     * Adds a new entry to the history.
     */
    fun add(entry: PreferenceHistoryEntry): PreferenceHistory =
        copy(entries = entries + entry)
}

/**
 * A single entry in the preference history.
 */
data class PreferenceHistoryEntry(
    val preference: PreferenceVector,
    val source: PreferenceSource,
    val cakeId: String? = null,
    val timestamp: Long = currentTimeMillis(),
    val notes: String? = null
)

/**
 * Source of a preference observation.
 */
enum class PreferenceSource {
    INITIAL,           // Initial profile setup
    USER_INPUT,        // Direct user input
    OBSERVED_CHOICE,   // Observed from actual cake choice
    SERENDIPITY,       // Learned from serendipity event
    API_INFERENCE      // Inferred from Gemini API
}

/**
 * Records what was learned from a serendipity event.
 */
data class LearningEntry(
    val dimension: PreferenceDimension,
    val previousValue: Double,
    val learnedValue: Double,
    val confidenceGain: Double,
    val sourceEvent: SerendipityEvent,
    val timestamp: Long = currentTimeMillis()
) {
    /**
     * The magnitude of the update.
     */
    val updateMagnitude: Double
        get() = kotlin.math.abs(learnedValue - previousValue)

    /**
     * Human-readable description of what was learned.
     */
    fun describe(): String {
        val direction = if (learnedValue > previousValue) "increased" else "decreased"
        return "Learned: ${dimension.name.lowercase()} preference $direction " +
                "from ${"%.2f".format(previousValue)} to ${"%.2f".format(learnedValue)}"
    }
}

/**
 * Extension to update the profile with new learning from serendipity.
 */
fun PartnerProfile.applyLearning(
    serendipityEvent: SerendipityEvent,
    learningRate: Double = 0.3
): PartnerProfile {
    val newLearnings = serendipityEvent.discoveredAspects.map { aspect ->
        LearningEntry(
            dimension = aspect.dimension,
            previousValue = aspect.expectedValue,
            learnedValue = aspect.actualValue,
            confidenceGain = aspect.surpriseLevel * learningRate,
            sourceEvent = serendipityEvent
        )
    }

    val updatedPreference = updatePreferenceFromAspects(
        currentPreference,
        serendipityEvent.discoveredAspects,
        learningRate
    )

    return copy(
        currentPreference = updatedPreference,
        preferenceHistory = preferenceHistory.add(
            PreferenceHistoryEntry(
                preference = updatedPreference,
                source = PreferenceSource.SERENDIPITY,
                timestamp = currentTimeMillis(),
                notes = "Updated from serendipity event with ${newLearnings.size} discoveries"
            )
        ),
        learningEntries = learningEntries + newLearnings,
        updatedAt = currentTimeMillis()
    )
}

/**
 * Helper to update preference vector from discovered aspects.
 */
private fun updatePreferenceFromAspects(
    current: PreferenceVector,
    aspects: List<DiscoveredAspect>,
    learningRate: Double
): PreferenceVector {
    var sweetness = current.sweetness
    var sourness = current.sourness
    var texture = current.texture
    var temperature = current.temperature
    var artistry = current.artistry

    for (aspect in aspects) {
        val delta = (aspect.actualValue - aspect.expectedValue) * learningRate
        when (aspect.dimension) {
            PreferenceDimension.SWEETNESS -> sweetness = (sweetness + delta).coerceIn(0.0, 1.0)
            PreferenceDimension.SOURNESS -> sourness = (sourness + delta).coerceIn(0.0, 1.0)
            PreferenceDimension.TEXTURE -> texture = (texture + delta).coerceIn(0.0, 1.0)
            PreferenceDimension.TEMPERATURE -> temperature = (temperature + delta).coerceIn(0.0, 1.0)
            PreferenceDimension.ARTISTRY -> artistry = (artistry + delta).coerceIn(0.0, 1.0)
        }
    }

    return PreferenceVector(sweetness, sourness, texture, temperature, artistry)
}
