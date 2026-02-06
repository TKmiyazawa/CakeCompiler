package com.example.cakecompiler.usecase

import com.example.cakecompiler.domain.model.HappinessScore
import com.example.cakecompiler.domain.model.HappinessWeights
import com.example.cakecompiler.domain.model.OverrideReason
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.RankedCake
import com.example.cakecompiler.domain.model.UserChoice
import com.example.cakecompiler.domain.model.canOverride
import com.example.cakecompiler.domain.model.chosenCakeId
import com.example.cakecompiler.domain.model.isAcceptance
import com.example.cakecompiler.domain.model.isOverride
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApplyUserOverrideUseCaseTest {

    private val useCase = ApplyUserOverrideUseCase()

    private val selfPref = PreferenceVector(0.8, 0.2, 0.5, 0.5, 0.5)
    private val partnerPref = PreferenceVector(0.2, 0.8, 0.5, 0.5, 0.5)

    private val recommendedCake = RankedCake(
        rank = 1,
        cakeId = "cake-1",
        cakeName = "Recommended Cake",
        score = HappinessScore.calculate(
            selfPref,
            partnerPref,
            PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5),
            HappinessWeights.DEFAULT
        )
    )

    @Test
    fun `canOverride always returns true`() {
        assertTrue(canOverride())
    }

    @Test
    fun `createOverride always succeeds`() {
        val override = useCase.createOverride(
            recommendation = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "User Choice",
            chosenCakeVector = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5),
            reason = OverrideReason.PersonalPreference
        )

        assertEquals("cake-2", override.chosenCakeId)
        assertEquals("User Choice", override.chosenCakeName)
        assertEquals(OverrideReason.PersonalPreference, override.reason)
    }

    @Test
    fun `applyChoice with acceptance returns Accepted result`() {
        val acceptance = UserChoice.AcceptRecommendation(recommendedCake)

        val result = useCase.applyChoice(recommendedCake, acceptance, selfPref, partnerPref)

        assertTrue(result is OverrideResult.Accepted)
        assertEquals("cake-1", result.finalCakeId)
    }

    @Test
    fun `applyChoice with override returns Overridden result`() {
        val chosenVector = PreferenceVector(1.0, 0.0, 0.5, 0.5, 0.5)
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "Different Cake",
            chosenCakeVector = chosenVector,
            reason = OverrideReason.Curiosity
        )

        val result = useCase.applyChoice(recommendedCake, override, selfPref, partnerPref)

        assertTrue(result is OverrideResult.Overridden)
        assertEquals("cake-2", result.finalCakeId)
        val overridden = result as OverrideResult.Overridden
        assertEquals(OverrideReason.Curiosity, overridden.reason)
    }

    @Test
    fun `overridden result includes score difference`() {
        val chosenVector = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0) // Poor match
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "Poor Match Cake",
            chosenCakeVector = chosenVector,
            reason = OverrideReason.Unspecified
        )

        val result = useCase.applyChoice(recommendedCake, override, selfPref, partnerPref)

        assertTrue(result is OverrideResult.Overridden)
        val overridden = result as OverrideResult.Overridden
        assertNotNull(overridden.scoreDifference)
    }

    @Test
    fun `override with significant divergence triggers serendipity`() {
        // Choose a cake very different from optimal
        val chosenVector = PreferenceVector(0.0, 1.0, 0.0, 1.0, 0.0)
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-3",
            chosenCakeName = "Surprising Choice",
            chosenCakeVector = chosenVector,
            reason = OverrideReason.Curiosity
        )

        val result = useCase.applyChoice(recommendedCake, override, selfPref, partnerPref)

        assertTrue(result is OverrideResult.Overridden)
        val overridden = result as OverrideResult.Overridden
        assertTrue(overridden.hasLearningOpportunity)
    }

    @Test
    fun `mightTriggerSerendipity returns true for large divergence`() {
        val expected = PreferenceVector(0.5, 0.5, 0.5, 0.5, 0.5)
        val actual = PreferenceVector(0.0, 1.0, 0.0, 1.0, 0.0)

        val mightTrigger = useCase.mightTriggerSerendipity(expected, actual)

        assertTrue(mightTrigger)
    }

    @Test
    fun `UserChoice isOverride extension works correctly`() {
        val acceptance = UserChoice.AcceptRecommendation(recommendedCake)
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "Other",
            chosenCakeVector = PreferenceVector.neutral(),
            reason = OverrideReason.Unspecified
        )

        assertTrue(!acceptance.isOverride())
        assertTrue(override.isOverride())
    }

    @Test
    fun `UserChoice isAcceptance extension works correctly`() {
        val acceptance = UserChoice.AcceptRecommendation(recommendedCake)
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "Other",
            chosenCakeVector = PreferenceVector.neutral(),
            reason = OverrideReason.Unspecified
        )

        assertTrue(acceptance.isAcceptance())
        assertTrue(!override.isAcceptance())
    }

    @Test
    fun `UserChoice chosenCakeId extension returns correct id`() {
        val acceptance = UserChoice.AcceptRecommendation(recommendedCake)
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "Other",
            chosenCakeVector = PreferenceVector.neutral(),
            reason = OverrideReason.Unspecified
        )

        assertEquals("cake-1", acceptance.chosenCakeId())
        assertEquals("cake-2", override.chosenCakeId())
    }

    @Test
    fun `OverrideResult wasOverridden extension works correctly`() {
        val acceptance = UserChoice.AcceptRecommendation(recommendedCake)
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "Other",
            chosenCakeVector = PreferenceVector.neutral(),
            reason = OverrideReason.Unspecified
        )

        val acceptedResult = useCase.applyChoice(recommendedCake, acceptance, selfPref, partnerPref)
        val overrideResult = useCase.applyChoice(recommendedCake, override, selfPref, partnerPref)

        assertTrue(!acceptedResult.wasOverridden())
        assertTrue(overrideResult.wasOverridden())
    }

    @Test
    fun `all override reasons are supported`() {
        val reasons = listOf(
            OverrideReason.Curiosity,
            OverrideReason.PartnerInsight("They mentioned liking chocolate"),
            OverrideReason.SpecialOccasion("Birthday"),
            OverrideReason.PersonalPreference,
            OverrideReason.ExternalConstraint("Budget"),
            OverrideReason.Unspecified
        )

        reasons.forEach { reason ->
            val override = useCase.createOverride(
                recommendation = recommendedCake,
                chosenCakeId = "cake-x",
                chosenCakeName = "Test",
                chosenCakeVector = PreferenceVector.neutral(),
                reason = reason
            )
            assertEquals(reason, override.reason)
        }
    }

    @Test
    fun `overridden result isLowerScore is true when score decreased`() {
        val chosenVector = PreferenceVector(0.0, 0.0, 0.0, 0.0, 0.0) // Very poor match
        val override = UserChoice.ManualOverride(
            recommendedCake = recommendedCake,
            chosenCakeId = "cake-2",
            chosenCakeName = "Poor Choice",
            chosenCakeVector = chosenVector,
            reason = OverrideReason.Unspecified
        )

        val result = useCase.applyChoice(recommendedCake, override, selfPref, partnerPref) as OverrideResult.Overridden

        assertTrue(result.isLowerScore)
    }
}
