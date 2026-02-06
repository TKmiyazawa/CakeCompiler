package com.example.cakecompiler.usecase

import com.example.cakecompiler.domain.model.HappinessWeights
import com.example.cakecompiler.domain.model.PreferenceVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateHappinessUseCaseTest {

    private val useCase = CalculateHappinessUseCase()

    @Test
    fun `calculateScore returns weighted sum of alignments`() {
        val selfPref = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)
        val partnerPref = PreferenceVector(0.0, 1.0, 0.5, 0.5, 0.5)
        val cake = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)

        val score = useCase.calculateScore(selfPref, partnerPref, cake)

        // selfAlignment = 1.0*0.5 + 0*0.5 + 0.5*0.5 + 0.5*0.5 + 0.5*0.5 = 0.5 + 0.75 = 1.25
        // partnerAlignment = 0*0.5 + 1.0*0.5 + 0.5*0.5 + 0.5*0.5 + 0.5*0.5 = 0.5 + 0.75 = 1.25
        // total = 0.2 * 1.25 + 0.8 * 1.25 = 1.25
        assertEquals(1.25, score.totalScore, 0.0001)
    }

    @Test
    fun `default weights prioritize partner happiness`() {
        val weights = HappinessWeights.DEFAULT

        assertTrue(weights.partnerWeight > weights.selfWeight)
        assertEquals(0.2, weights.selfWeight)
        assertEquals(0.8, weights.partnerWeight)
    }

    @Test
    fun `calculateScore with equal weights distributes evenly`() {
        val selfPref = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)
        val partnerPref = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val cake = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)

        val score = useCase.calculateScore(selfPref, partnerPref, cake, HappinessWeights.EQUAL)

        // selfAlignment = 5 * 0.5 = 2.5
        // partnerAlignment = 0
        // total = 0.5 * 2.5 + 0.5 * 0 = 1.25
        assertEquals(1.25, score.totalScore, 0.0001)
    }

    @Test
    fun `findOptimalCakePreference blends preferences by weights`() {
        val selfPref = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)
        val partnerPref = PreferenceVector(0.0, 1.0, 0.5, 0.5, 0.5)

        val optimal = useCase.findOptimalCakePreference(selfPref, partnerPref)

        // With default weights (0.2 self, 0.8 partner):
        // sweetness = 0.2 * 1.0 + 0.8 * 0.0 = 0.2
        // sourness = 0.2 * 0.0 + 0.8 * 1.0 = 0.8
        assertEquals(0.2, optimal.sweetness, 0.0001)
        assertEquals(0.8, optimal.sourness, 0.0001)
    }

    @Test
    fun `findOptimalCakePreference with equal weights gives midpoint`() {
        val selfPref = PreferenceVector(1.0, 0.0, 0.0, 0.0, 0.0)
        val partnerPref = PreferenceVector(0.0, 1.0, 1.0, 1.0, 1.0)

        val optimal = useCase.findOptimalCakePreference(selfPref, partnerPref, HappinessWeights.EQUAL)

        assertEquals(0.5, optimal.sweetness, 0.0001)
        assertEquals(0.5, optimal.sourness, 0.0001)
    }

    @Test
    fun `rankCakes returns cakes sorted by score descending`() {
        val selfPref = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)
        val partnerPref = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)

        val cakes = listOf(
            CakeCandidate("1", "Low Match", PreferenceVector(0.0, 1.0, 0.5, 0.5, 0.5)),
            CakeCandidate("2", "Perfect Match", PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)),
            CakeCandidate("3", "Medium Match", PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5))
        )

        val ranking = useCase.rankCakes(selfPref, partnerPref, cakes)

        assertEquals(3, ranking.rankings.size)
        assertEquals("2", ranking.topChoice?.cakeId) // Perfect match should be first
        assertEquals(1, ranking.rankings[0].rank)
        assertEquals(2, ranking.rankings[1].rank)
        assertEquals(3, ranking.rankings[2].rank)
    }

    @Test
    fun `rankCakes returns empty ranking for empty input`() {
        val selfPref = PreferenceVector.neutral()
        val partnerPref = PreferenceVector.neutral()

        val ranking = useCase.rankCakes(selfPref, partnerPref, emptyList())

        assertTrue(ranking.isEmpty)
        assertEquals(null, ranking.topChoice)
    }

    @Test
    fun `calculateMaxPossibleScore equals score of optimal cake`() {
        val selfPref = PreferenceVector(0.8, 0.2, 0.5, 0.7, 0.3)
        val partnerPref = PreferenceVector(0.3, 0.7, 0.6, 0.4, 0.8)

        val maxScore = useCase.calculateMaxPossibleScore(selfPref, partnerPref)
        val optimalCake = useCase.findOptimalCakePreference(selfPref, partnerPref)
        val optimalScore = useCase.calculateScore(selfPref, partnerPref, optimalCake)

        assertEquals(maxScore, optimalScore.totalScore, 0.0001)
    }

    @Test
    fun `evaluateOptimality returns 1 for optimal cake`() {
        val selfPref = PreferenceVector(0.8, 0.2, 0.5, 0.7, 0.3)
        val partnerPref = PreferenceVector(0.3, 0.7, 0.6, 0.4, 0.8)
        val optimalCake = useCase.findOptimalCakePreference(selfPref, partnerPref)

        val optimality = useCase.evaluateOptimality(selfPref, partnerPref, optimalCake)

        assertEquals(1.0, optimality, 0.0001)
    }

    @Test
    fun `evaluateOptimality returns lower value for suboptimal cake`() {
        val selfPref = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)
        val partnerPref = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)
        val suboptimalCake = PreferenceVector(0.0, 1.0, 0.5, 0.5, 0.5) // Opposite preference

        val optimality = useCase.evaluateOptimality(selfPref, partnerPref, suboptimalCake)

        assertTrue(optimality < 1.0)
    }

    @Test
    fun `score breakdown shows individual contributions`() {
        val selfPref = PreferenceVector(1.0, 0.0, 0.0, 0.0, 0.0)
        val partnerPref = PreferenceVector(0.0, 1.0, 0.0, 0.0, 0.0)
        val cake = PreferenceVector(1.0, 0.0, 0.0, 0.0, 0.0) // Matches self only

        val score = useCase.calculateScore(selfPref, partnerPref, cake)

        assertEquals(1.0, score.selfAlignment) // Perfect self match
        assertEquals(0.0, score.partnerAlignment) // No partner match
        assertTrue(score.selfContribution > score.partnerContribution)
    }

    @Test
    fun `score isPartnerFavored is true when partner alignment higher`() {
        val selfPref = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0)
        val partnerPref = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0)
        val cake = PreferenceVector(1.0, 1.0, 1.0, 1.0, 1.0) // Matches partner

        val score = useCase.calculateScore(selfPref, partnerPref, cake)

        assertTrue(score.isPartnerFavored())
    }

    @Test
    fun `partner priority ratio is 1_5 for default weights`() {
        val weights = HappinessWeights.DEFAULT
        val ratio = weights.partnerPriorityRatio()

        assertEquals(4.0, ratio, 0.0001) // 0.8 / 0.2 = 4.0
    }
}
