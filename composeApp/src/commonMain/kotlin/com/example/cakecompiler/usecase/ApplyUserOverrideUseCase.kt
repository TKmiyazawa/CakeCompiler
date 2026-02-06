package com.example.cakecompiler.usecase

import com.example.cakecompiler.domain.model.HappinessScore
import com.example.cakecompiler.domain.model.HappinessWeights
import com.example.cakecompiler.domain.model.OverrideReason
import com.example.cakecompiler.domain.model.PreferenceVector
import com.example.cakecompiler.domain.model.RankedCake
import com.example.cakecompiler.domain.model.UserChoice
import com.example.cakecompiler.domain.model.canOverride
import com.example.cakecompiler.domain.model.currentTimeMillis

/**
 * Use case for handling user overrides of recommendations.
 *
 * CRITICAL ETHICAL PRINCIPLE: Users can ALWAYS override.
 * The system provides recommendations, but the user has final authority.
 */
class ApplyUserOverrideUseCase(
    private val calculateHappiness: CalculateHappinessUseCase = CalculateHappinessUseCase(),
    private val observeSerendipity: ObserveSerendipityAnomalyUseCase = ObserveSerendipityAnomalyUseCase()
) {

    /**
     * Processes a user's choice (accept or override).
     *
     * @param recommendation The system's recommended cake
     * @param userChoice The user's final choice
     * @param selfPreference Current self preference
     * @param partnerPreference Current partner preference
     * @return Result of applying the choice
     */
    fun applyChoice(
        recommendation: RankedCake,
        userChoice: UserChoice,
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector
    ): OverrideResult {
        return when (userChoice) {
            is UserChoice.AcceptRecommendation -> handleAcceptance(
                recommendation = recommendation,
                selfPreference = selfPreference,
                partnerPreference = partnerPreference
            )
            is UserChoice.ManualOverride -> handleOverride(
                recommendation = recommendation,
                override = userChoice,
                selfPreference = selfPreference,
                partnerPreference = partnerPreference
            )
        }
    }

    /**
     * Creates an override choice from user input.
     * This method always succeeds because overrides are ALWAYS allowed.
     *
     * @param recommendation The original recommendation
     * @param chosenCakeId The cake ID the user chose
     * @param chosenCakeName The cake name
     * @param chosenCakeVector The preference vector of the chosen cake
     * @param reason The reason for the override (optional)
     * @return The constructed ManualOverride choice
     */
    fun createOverride(
        recommendation: RankedCake,
        chosenCakeId: String,
        chosenCakeName: String,
        chosenCakeVector: PreferenceVector,
        reason: OverrideReason = OverrideReason.Unspecified
    ): UserChoice.ManualOverride {
        // Assert the ethical principle
        require(canOverride()) { "Override should always be allowed - ethical violation!" }

        return UserChoice.ManualOverride(
            recommendedCake = recommendation,
            chosenCakeId = chosenCakeId,
            chosenCakeName = chosenCakeName,
            chosenCakeVector = chosenCakeVector,
            reason = reason
        )
    }

    /**
     * Checks if an override might trigger a serendipity event.
     * This is useful for deciding whether to update the preference model.
     *
     * @param expectedPreference What we expected the user to prefer
     * @param actualChoice What the user actually chose
     * @return True if this override suggests a significant preference change
     */
    fun mightTriggerSerendipity(
        expectedPreference: PreferenceVector,
        actualChoice: PreferenceVector
    ): Boolean {
        val serendipity = observeSerendipity.detectSerendipity(expectedPreference, actualChoice)
        return serendipity != null
    }

    private fun handleAcceptance(
        recommendation: RankedCake,
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector
    ): OverrideResult.Accepted {
        return OverrideResult.Accepted(
            finalCakeId = recommendation.cakeId,
            finalCakeName = recommendation.cakeName,
            originalScore = recommendation.score,
            timestamp = currentTimeMillis()
        )
    }

    private fun handleOverride(
        recommendation: RankedCake,
        override: UserChoice.ManualOverride,
        selfPreference: PreferenceVector,
        partnerPreference: PreferenceVector
    ): OverrideResult.Overridden {
        // Calculate the score for the chosen cake
        val chosenScore = calculateHappiness.calculateScore(
            selfPreference = selfPreference,
            partnerPreference = partnerPreference,
            cakeVector = override.chosenCakeVector
        )

        // Check if this triggers serendipity (learning opportunity)
        val optimalPreference = calculateHappiness.findOptimalCakePreference(
            selfPreference = selfPreference,
            partnerPreference = partnerPreference
        )
        val serendipityEvent = observeSerendipity.detectSerendipity(
            expectedPreference = optimalPreference,
            actualPreference = override.chosenCakeVector
        )

        // Calculate score difference
        val scoreDifference = recommendation.score.totalScore - chosenScore.totalScore

        return OverrideResult.Overridden(
            finalCakeId = override.chosenCakeId,
            finalCakeName = override.chosenCakeName,
            originalRecommendation = recommendation,
            chosenScore = chosenScore,
            scoreDifference = scoreDifference,
            reason = override.reason,
            triggeredSerendipity = serendipityEvent,
            timestamp = currentTimeMillis()
        )
    }
}

/**
 * Result of applying a user choice.
 */
sealed class OverrideResult {
    abstract val finalCakeId: String
    abstract val finalCakeName: String
    abstract val timestamp: Long

    /**
     * User accepted the recommendation.
     */
    data class Accepted(
        override val finalCakeId: String,
        override val finalCakeName: String,
        val originalScore: HappinessScore,
        override val timestamp: Long
    ) : OverrideResult()

    /**
     * User overrode the recommendation.
     */
    data class Overridden(
        override val finalCakeId: String,
        override val finalCakeName: String,
        val originalRecommendation: RankedCake,
        val chosenScore: HappinessScore,
        val scoreDifference: Double,
        val reason: OverrideReason,
        val triggeredSerendipity: com.example.cakecompiler.domain.model.SerendipityEvent?,
        override val timestamp: Long
    ) : OverrideResult() {

        /**
         * Returns true if the override resulted in a lower calculated score.
         * Note: This doesn't mean it was a "bad" choice - the user knows best!
         */
        val isLowerScore: Boolean
            get() = scoreDifference > 0

        /**
         * Returns true if this override triggered a learning opportunity.
         */
        val hasLearningOpportunity: Boolean
            get() = triggeredSerendipity != null
    }
}

/**
 * Extension to check if the result was an override.
 */
fun OverrideResult.wasOverridden(): Boolean = this is OverrideResult.Overridden
