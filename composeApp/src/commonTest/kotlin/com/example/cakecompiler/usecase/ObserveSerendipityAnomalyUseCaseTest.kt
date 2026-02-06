package com.example.cakecompiler.usecase

import com.example.cakecompiler.domain.model.PreferenceDimension
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.SerendipityEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObserveSerendipityAnomalyUseCaseTest {

    private val useCase = ObserveSerendipityAnomalyUseCase()

    @Test
    fun `detectSerendipity returns null when divergence below threshold`() {
        val expected = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.6, 0.5, 0.5, 0.5, 0.5) // Small difference

        val result = useCase.detectSerendipity(expected, actual)

        assertNull(result)
    }

    @Test
    fun `detectSerendipity returns event when divergence exceeds threshold`() {
        val expected = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val actual = PreferenceVector(0.5, 0.5, 0.5, 0.0, 0.0) // Significant difference

        val result = useCase.detectSerendipity(expected, actual)

        assertNotNull(result)
        assertTrue(result.divergenceScore >= SerendipityEvent.THRESHOLD_OF_SURPRISE)
    }

    @Test
    fun `detectSerendipity identifies diverged dimensions`() {
        val expected = PreferenceVector(0.0, 0.5, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.8, 0.5, 0.5, 0.5, 0.5) // Only sweetness diverged

        val result = useCase.detectSerendipity(expected, actual)

        assertNotNull(result)
        assertTrue(result.discoveredAspects.any { it.dimension == PreferenceDimension.SWEETNESS })
    }

    @Test
    fun `serendipity threshold is 0_5`() {
        assertEquals(0.5, SerendipityEvent.THRESHOLD_OF_SURPRISE)
    }

    @Test
    fun `calculateDivergence returns zero for identical vectors`() {
        val vector = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)

        val divergence = useCase.calculateDivergence(vector, vector)

        assertEquals(0.0, divergence)
    }

    @Test
    fun `calculateDivergence returns correct Euclidean distance`() {
        val v1 = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val v2 = PreferenceVector(1.0, 0.0, 0.0, 0.0, 0.0)

        val divergence = useCase.calculateDivergence(v1, v2)

        assertEquals(1.0, divergence, 0.0001)
    }

    @Test
    fun `getDivergenceWarningLevel returns 0 for identical vectors`() {
        val vector = PreferenceVector.neutral()

        val warning = useCase.getDivergenceWarningLevel(vector, vector)

        assertEquals(0.0, warning)
    }

    @Test
    fun `getDivergenceWarningLevel returns 1 at threshold`() {
        val expected = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        // Create a vector exactly at threshold distance (0.5)
        val actual = PreferenceVector(0.5, 0.0, 0.0, 0.0, 0.0)

        val warning = useCase.getDivergenceWarningLevel(expected, actual)

        assertEquals(1.0, warning, 0.0001)
    }

    @Test
    fun `analyzeSerendipity returns moderate significance for mid-range divergence`() {
        // Divergence = 0.6, which is > THRESHOLD_OF_SURPRISE (0.5) but < STRONG_THRESHOLD (0.7)
        val expected = PreferenceVector(0.0, 0.5, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.6, 0.5, 0.5, 0.5, 0.5)

        val event = useCase.detectSerendipity(expected, actual)
        assertNotNull(event)

        val analysis = useCase.analyzeSerendipity(event)

        assertEquals(SignificanceLevel.MODERATE, analysis.significanceLevel)
    }

    @Test
    fun `analyzeSerendipity returns high significance for large divergence`() {
        val expected = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val actual = PreferenceVector(1.0, 1.0, 0.0, 0.0, 0.0)

        val event = useCase.detectSerendipity(expected, actual)
        assertNotNull(event)

        val analysis = useCase.analyzeSerendipity(event)

        assertEquals(SignificanceLevel.HIGH, analysis.significanceLevel)
    }

    @Test
    fun `analyzeSerendipity always triggers learning`() {
        val expected = PreferenceVector(0.0, 0.0, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.6, 0.6, 0.5, 0.5, 0.5)

        val event = useCase.detectSerendipity(expected, actual)
        assertNotNull(event)

        val analysis = useCase.analyzeSerendipity(event)

        assertTrue(analysis.shouldTriggerLearning)
    }

    @Test
    fun `analyzeSerendipity provides insights for each diverged dimension`() {
        val expected = PreferenceVector(0.0, 0.0, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)

        val event = useCase.detectSerendipity(expected, actual)
        assertNotNull(event)

        val analysis = useCase.analyzeSerendipity(event)

        assertTrue(analysis.insights.isNotEmpty())
        analysis.insights.forEach { insight ->
            assertTrue(insight.insight.isNotEmpty())
        }
    }

    @Test
    fun `serendipity event isStrong returns true for high divergence`() {
        val expected = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val actual = PreferenceVector(1.0, 1.0, 0.0, 0.0, 0.0)

        val event = useCase.detectSerendipity(expected, actual)
        assertNotNull(event)

        assertTrue(event.isStrong())
    }

    @Test
    fun `serendipity event mostSurprisingAspect returns highest surprise`() {
        val expected = PreferenceVector(0.0, 0.5, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.8, 0.5, 0.5, 0.5, 0.5) // Only sweetness diverged significantly

        val event = useCase.detectSerendipity(expected, actual)
        assertNotNull(event)

        val mostSurprising = event.mostSurprisingAspect()
        assertNotNull(mostSurprising)
        assertEquals(PreferenceDimension.SWEETNESS, mostSurprising.dimension)
    }

    @Test
    fun `discovered aspect describes direction correctly`() {
        val expected = PreferenceVector(0.2, 0.5, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.8, 0.5, 0.5, 0.5, 0.5)

        val event = useCase.detectSerendipity(expected, actual)
        assertNotNull(event)

        val sweetnessAspect = event.discoveredAspects.find { it.dimension == PreferenceDimension.SWEETNESS }
        assertNotNull(sweetnessAspect)
        assertEquals(com.example.cakecompiler.domain.model.SurpriseDirection.HIGHER_THAN_EXPECTED, sweetnessAspect.direction)
    }

    @Test
    fun `max divergence constant is sqrt of 5`() {
        assertEquals(kotlin.math.sqrt(5.0), SerendipityEvent.MAX_DIVERGENCE, 0.0001)
    }
}
