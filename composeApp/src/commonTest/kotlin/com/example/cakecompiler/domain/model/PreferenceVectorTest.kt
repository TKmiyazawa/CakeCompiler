package com.example.cakecompiler.domain.model

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PreferenceVectorTest {

    @Test
    fun `creates valid vector with all values in range`() {
        val vector = PreferenceVector(0.5, 0.3, 0.8, 0.1, 0.9)

        assertEquals(0.5, vector.sweetness)
        assertEquals(0.3, vector.sourness)
        assertEquals(0.8, vector.texture)
        assertEquals(0.1, vector.temperature)
        assertEquals(0.9, vector.artistry)
    }

    @Test
    fun `accepts boundary values 0 and 1`() {
        val vectorZero = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val vectorOne = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)

        assertEquals(0.0, vectorZero.sweetness)
        assertEquals(1.0, vectorOne.sweetness)
    }

    @Test
    fun `rejects values below 0`() {
        assertFailsWith<IllegalArgumentException> {
            PreferenceVector(-0.1, 0.5, 0.5, 0.5, 0.5)
        }
    }

    @Test
    fun `rejects values above 1`() {
        assertFailsWith<IllegalArgumentException> {
            PreferenceVector(0.5, 1.1, 0.5, 0.5, 0.5)
        }
    }

    @Test
    fun `dot product of identical vectors equals sum of squares`() {
        val vector = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)
        val expected = 0.5 * 0.5 * 5 // 5 dimensions, each 0.25

        assertEquals(expected, vector.dot(vector), 0.0001)
    }

    @Test
    fun `dot product of orthogonal-like vectors is smaller`() {
        val v1 = PreferenceVector(1.0, 0.0, 0.0, 0.0, 0.0)
        val v2 = PreferenceVector(0.0, 1.0, 0.0, 0.0, 0.0)

        assertEquals(0.0, v1.dot(v2))
    }

    @Test
    fun `dot product is commutative`() {
        val v1 = PreferenceVector(0.3, 0.5, 0.7, 0.2, 0.9)
        val v2 = PreferenceVector(0.8, 0.4, 0.1, 0.6, 0.3)

        assertEquals(v1.dot(v2), v2.dot(v1), 0.0001)
    }

    @Test
    fun `distance to same vector is zero`() {
        val vector = PreferenceVector(0.5, 0.3, 0.8, 0.1, 0.9)

        assertEquals(0.0, vector.distanceTo(vector))
    }

    @Test
    fun `distance is symmetric`() {
        val v1 = PreferenceVector(0.2, 0.4, 0.6, 0.8, 1.0)
        val v2 = PreferenceVector(0.8, 0.6, 0.4, 0.2, 0.0)

        assertEquals(v1.distanceTo(v2), v2.distanceTo(v1), 0.0001)
    }

    @Test
    fun `maximum distance is sqrt of 5`() {
        val v1 = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val v2 = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)

        assertEquals(sqrt(5.0), v1.distanceTo(v2), 0.0001)
    }

    @Test
    fun `magnitude of zero vector is zero`() {
        val vector = PreferenceVector.zero()

        assertEquals(0.0, vector.magnitude())
    }

    @Test
    fun `magnitude of unit corner is sqrt of 5`() {
        val vector = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)

        assertEquals(sqrt(5.0), vector.magnitude(), 0.0001)
    }

    @Test
    fun `blend with equal weights gives midpoint`() {
        val v1 = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val v2 = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)

        val blended = v1.blend(v2, 0.5, 0.5)

        assertEquals(0.5, blended.sweetness, 0.0001)
        assertEquals(0.5, blended.sourness, 0.0001)
        assertEquals(0.5, blended.texture, 0.0001)
        assertEquals(0.5, blended.temperature, 0.0001)
        assertEquals(0.5, blended.artistry, 0.0001)
    }

    @Test
    fun `blend with full self weight returns self`() {
        val v1 = PreferenceVector(0.3, 0.5, 0.7, 0.2, 0.8)
        val v2 = PreferenceVector(0.9, 0.1, 0.4, 0.6, 0.3)

        val blended = v1.blend(v2, 1.0, 0.0)

        assertEquals(v1.sweetness, blended.sweetness, 0.0001)
        assertEquals(v1.sourness, blended.sourness, 0.0001)
    }

    @Test
    fun `blend with full other weight returns other`() {
        val v1 = PreferenceVector(0.3, 0.5, 0.7, 0.2, 0.8)
        val v2 = PreferenceVector(0.9, 0.1, 0.4, 0.6, 0.3)

        val blended = v1.blend(v2, 0.0, 1.0)

        assertEquals(v2.sweetness, blended.sweetness, 0.0001)
        assertEquals(v2.sourness, blended.sourness, 0.0001)
    }

    @Test
    fun `toList returns all five dimensions in order`() {
        val vector = PreferenceVector(0.1, 0.2, 0.3, 0.4, 0.5)
        val list = vector.toList()

        assertEquals(5, list.size)
        assertEquals(0.1, list[0])
        assertEquals(0.2, list[1])
        assertEquals(0.3, list[2])
        assertEquals(0.4, list[3])
        assertEquals(0.5, list[4])
    }

    @Test
    fun `neutral vector has all dimensions at 0_5`() {
        val neutral = PreferenceVector.neutral()

        assertEquals(0.5, neutral.sweetness)
        assertEquals(0.5, neutral.sourness)
        assertEquals(0.5, neutral.texture)
        assertEquals(0.5, neutral.temperature)
        assertEquals(0.5, neutral.artistry)
    }

    @Test
    fun `get extension returns correct dimension value`() {
        val vector = PreferenceVector(0.1, 0.2, 0.3, 0.4, 0.5)

        assertEquals(0.1, vector.get(PreferenceDimension.SWEETNESS))
        assertEquals(0.2, vector.get(PreferenceDimension.SOURNESS))
        assertEquals(0.3, vector.get(PreferenceDimension.TEXTURE))
        assertEquals(0.4, vector.get(PreferenceDimension.TEMPERATURE))
        assertEquals(0.5, vector.get(PreferenceDimension.ARTISTRY))
    }

    @Test
    fun `dimensions list contains all five dimensions`() {
        val dimensions = PreferenceVector.DIMENSIONS

        assertEquals(5, dimensions.size)
        assertTrue(dimensions.contains(PreferenceDimension.SWEETNESS))
        assertTrue(dimensions.contains(PreferenceDimension.SOURNESS))
        assertTrue(dimensions.contains(PreferenceDimension.TEXTURE))
        assertTrue(dimensions.contains(PreferenceDimension.TEMPERATURE))
        assertTrue(dimensions.contains(PreferenceDimension.ARTISTRY))
    }
}
