package com.example.cakecompiler.presentation.effect

import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.usecase.CakeCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SerendipityShakeEffectTest {

    private val effect = SerendipityShakeEffect()

    @Test
    fun `findMostDivergentCake returns NoCandidates for empty list`() {
        val result = effect.findMostDivergentCake(
            optimalPreference = PreferenceVector.neutral(),
            candidates = emptyList()
        )

        assertEquals(SerendipityResult.NoCandidates, result)
    }

    @Test
    fun `findMostDivergentCake finds cake with highest divergence`() {
        val optimal = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val candidates = listOf(
            CakeCandidate("close", "Close Cake", PreferenceVector(0.1, 0.1, 0.1, 0.1, 0.1)),
            CakeCandidate("far", "Far Cake", PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)),
            CakeCandidate("medium", "Medium Cake", PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5))
        )

        val result = effect.findMostDivergentCake(optimal, candidates)

        assertTrue(result is SerendipityResult.Found)
        assertEquals("far", (result as SerendipityResult.Found).cake.id)
    }

    @Test
    fun `findMostDivergentCake calculates correct divergence score`() {
        val optimal = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val candidates = listOf(
            CakeCandidate("far", "Far Cake", PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0))
        )

        val result = effect.findMostDivergentCake(optimal, candidates)

        assertTrue(result is SerendipityResult.Found)
        val found = result as SerendipityResult.Found
        // Distance should be sqrt(5) ≈ 2.236
        assertEquals(kotlin.math.sqrt(5.0), found.divergenceScore, 0.001)
    }

    @Test
    fun `normalizedDivergence is between 0 and 1`() {
        val optimal = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)
        val candidates = listOf(
            CakeCandidate("cake", "Cake", PreferenceVector(0.0, 1.0, 0.0, 1.0, 0.0))
        )

        val result = effect.findMostDivergentCake(optimal, candidates)

        assertTrue(result is SerendipityResult.Found)
        val found = result as SerendipityResult.Found
        assertTrue(found.normalizedDivergence in 0.0..1.0)
    }

    @Test
    fun `surprisePercentage scales with divergence`() {
        val optimal = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val maxDivergent = listOf(
            CakeCandidate("max", "Max", PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0))
        )
        val midDivergent = listOf(
            CakeCandidate("mid", "Mid", PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5))
        )

        val maxResult = effect.findMostDivergentCake(optimal, maxDivergent) as SerendipityResult.Found
        val midResult = effect.findMostDivergentCake(optimal, midDivergent) as SerendipityResult.Found

        assertTrue(maxResult.surprisePercentage > midResult.surprisePercentage)
    }

    @Test
    fun `surpriseLevel reflects divergence magnitude`() {
        val optimal = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)

        // Very surprising (high divergence)
        val verySurprising = listOf(
            CakeCandidate("vs", "VS", PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0))
        )
        val vsResult = effect.findMostDivergentCake(optimal, verySurprising) as SerendipityResult.Found
        assertEquals(SurpriseLevel.VERY_SURPRISING, vsResult.surpriseLevel)

        // Mild (low divergence)
        val mild = listOf(
            CakeCandidate("mild", "Mild", PreferenceVector(0.2, 0.2, 0.2, 0.2, 0.2))
        )
        val mildResult = effect.findMostDivergentCake(optimal, mild) as SerendipityResult.Found
        assertTrue(mildResult.surpriseLevel in listOf(SurpriseLevel.MILD, SurpriseLevel.SOMEWHAT_SURPRISING))
    }

    @Test
    fun `selectRandomDivergentCake respects weight distribution`() {
        val optimal = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val candidates = listOf(
            CakeCandidate("close", "Close", PreferenceVector(0.1, 0.1, 0.1, 0.1, 0.1)),
            CakeCandidate("far", "Far", PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0))
        )

        // With random value near 1.0, should prefer far cake (higher weight)
        val result = effect.selectRandomDivergentCake(optimal, candidates, randomValue = 0.99)

        assertTrue(result is SerendipityResult.Found)
        // Far cake has much higher weight, should be selected with high random value
        assertEquals("far", (result as SerendipityResult.Found).cake.id)
    }

    @Test
    fun `selectRandomDivergentCake returns NoCandidates for empty list`() {
        val result = effect.selectRandomDivergentCake(
            optimalPreference = PreferenceVector.neutral(),
            candidates = emptyList()
        )

        assertEquals(SerendipityResult.NoCandidates, result)
    }

    @Test
    fun `filterUnusualCakes returns all when no past choices`() {
        val candidates = listOf(
            CakeCandidate("1", "One", PreferenceVector(0.2, 0.2, 0.2, 0.2, 0.2)),
            CakeCandidate("2", "Two", PreferenceVector(0.8, 0.8, 0.8, 0.8, 0.8))
        )

        val unusual = effect.filterUnusualCakes(candidates, emptyList())

        assertEquals(2, unusual.size)
    }

    @Test
    fun `filterUnusualCakes excludes similar to past choices`() {
        val candidates = listOf(
            CakeCandidate("similar", "Similar", PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)),
            CakeCandidate("different", "Different", PreferenceVector(0.0, 1.0, 0.0, 1.0, 0.0))
        )
        val pastChoices = listOf(
            PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)
        )

        val unusual = effect.filterUnusualCakes(candidates, pastChoices, threshold = 0.5)

        assertTrue(unusual.any { it.id == "different" })
        assertTrue(unusual.none { it.id == "similar" })
    }

    @Test
    fun `findMostDivergentCake triggers serendipity when divergence high`() {
        val optimal = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val candidates = listOf(
            CakeCandidate("divergent", "Divergent", PreferenceVector(0.8, 0.8, 0.8, 0.8, 0.8))
        )

        val result = effect.findMostDivergentCake(optimal, candidates) as SerendipityResult.Found

        // Serendipity should be triggered for high divergence
        assertNotNull(result.forcedSerendipityEvent)
    }

    @Test
    fun `findMostDivergentCake may not trigger serendipity for low divergence`() {
        val optimal = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)
        val candidates = listOf(
            CakeCandidate("similar", "Similar", PreferenceVector(0.6, 0.5, 0.5, 0.5, 0.5))
        )

        val result = effect.findMostDivergentCake(optimal, candidates) as SerendipityResult.Found

        // Low divergence should not trigger serendipity
        assertNull(result.forcedSerendipityEvent)
    }

    @Test
    fun `mathematical model integrity - divergence calculation matches domain model`() {
        val v1 = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val v2 = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)

        val candidates = listOf(CakeCandidate("test", "Test", v2))
        val result = effect.findMostDivergentCake(v1, candidates) as SerendipityResult.Found

        // Verify the divergence matches the domain model's distance calculation
        val expectedDistance = v1.distanceTo(v2)
        assertEquals(expectedDistance, result.divergenceScore, 0.0001)
    }
}
