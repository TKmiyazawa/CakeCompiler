package com.example.cakecompiler.domain.model

import kotlin.math.sqrt

/**
 * A 5-dimensional preference vector representing cake characteristics.
 * Each dimension is normalized to [0.0, 1.0].
 *
 * Dimensions:
 * - sweetness: How sweet the cake is
 * - sourness: How sour/tangy the cake is
 * - texture: Softness vs crunchiness (0=soft, 1=crunchy)
 * - temperature: Cold vs hot (0=cold, 1=hot)
 * - artistry: Visual presentation quality
 */
data class PreferenceVector(
    val sweetness: Double,
    val sourness: Double,
    val texture: Double,
    val temperature: Double,
    val artistry: Double
) {
    init {
        require(sweetness in 0.0..1.0) { "sweetness must be in [0.0, 1.0], got $sweetness" }
        require(sourness in 0.0..1.0) { "sourness must be in [0.0, 1.0], got $sourness" }
        require(texture in 0.0..1.0) { "texture must be in [0.0, 1.0], got $texture" }
        require(temperature in 0.0..1.0) { "temperature must be in [0.0, 1.0], got $temperature" }
        require(artistry in 0.0..1.0) { "artistry must be in [0.0, 1.0], got $artistry" }
    }

    /**
     * Returns the vector as a list of doubles for iteration.
     */
    fun toList(): List<Double> = listOf(sweetness, sourness, texture, temperature, artistry)

    /**
     * Calculates the dot product with another vector.
     * Used to measure alignment between preferences.
     */
    fun dot(other: PreferenceVector): Double {
        return sweetness * other.sweetness +
                sourness * other.sourness +
                texture * other.texture +
                temperature * other.temperature +
                artistry * other.artistry
    }

    /**
     * Calculates the Euclidean distance to another vector.
     * Used for divergence/serendipity detection.
     * Max possible value in 5D unit cube: √5 ≈ 2.236
     */
    fun distanceTo(other: PreferenceVector): Double {
        val dSweet = sweetness - other.sweetness
        val dSour = sourness - other.sourness
        val dText = texture - other.texture
        val dTemp = temperature - other.temperature
        val dArt = artistry - other.artistry
        return sqrt(dSweet * dSweet + dSour * dSour + dText * dText + dTemp * dTemp + dArt * dArt)
    }

    /**
     * Calculates the L2 norm (magnitude) of the vector.
     */
    fun magnitude(): Double {
        return sqrt(sweetness * sweetness + sourness * sourness + texture * texture + temperature * temperature + artistry * artistry)
    }

    /**
     * Calculates the weighted blend of this vector with another.
     * Used to find optimal cake preferences balancing two people.
     */
    fun blend(other: PreferenceVector, selfWeight: Double, otherWeight: Double): PreferenceVector {
        require(selfWeight >= 0 && otherWeight >= 0) { "Weights must be non-negative" }
        val totalWeight = selfWeight + otherWeight
        require(totalWeight > 0) { "Total weight must be positive" }

        val w1 = selfWeight / totalWeight
        val w2 = otherWeight / totalWeight

        return PreferenceVector(
            sweetness = (sweetness * w1 + other.sweetness * w2).coerceIn(0.0, 1.0),
            sourness = (sourness * w1 + other.sourness * w2).coerceIn(0.0, 1.0),
            texture = (texture * w1 + other.texture * w2).coerceIn(0.0, 1.0),
            temperature = (temperature * w1 + other.temperature * w2).coerceIn(0.0, 1.0),
            artistry = (artistry * w1 + other.artistry * w2).coerceIn(0.0, 1.0)
        )
    }

    companion object {
        val DIMENSIONS = listOf(
            PreferenceDimension.SWEETNESS,
            PreferenceDimension.SOURNESS,
            PreferenceDimension.TEXTURE,
            PreferenceDimension.TEMPERATURE,
            PreferenceDimension.ARTISTRY
        )

        fun zero(): PreferenceVector = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)

        fun neutral(): PreferenceVector = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)
    }
}

/**
 * Enumeration of preference dimensions for type-safe references.
 */
enum class PreferenceDimension {
    SWEETNESS,
    SOURNESS,
    TEXTURE,
    TEMPERATURE,
    ARTISTRY
}

/**
 * Extension to get a specific dimension value from a PreferenceVector.
 */
fun PreferenceVector.get(dimension: PreferenceDimension): Double = when (dimension) {
    PreferenceDimension.SWEETNESS -> sweetness
    PreferenceDimension.SOURNESS -> sourness
    PreferenceDimension.TEXTURE -> texture
    PreferenceDimension.TEMPERATURE -> temperature
    PreferenceDimension.ARTISTRY -> artistry
}
