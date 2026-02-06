package com.example.cakecompiler.domain.model

/**
 * Represents the user's choice in response to a cake recommendation.
 * This is a sealed class to ensure type-safe handling of all cases.
 */
sealed class UserChoice {
    /**
     * User accepts the system's recommendation.
     */
    data class AcceptRecommendation(
        val recommendedCake: RankedCake,
        val timestamp: Long = currentTimeMillis()
    ) : UserChoice()

    /**
     * User manually overrides the recommendation.
     * This is ALWAYS allowed - the ethical principle of user autonomy.
     */
    data class ManualOverride(
        val recommendedCake: RankedCake,
        val chosenCakeId: String,
        val chosenCakeName: String,
        val chosenCakeVector: PreferenceVector,
        val reason: OverrideReason,
        val timestamp: Long = currentTimeMillis()
    ) : UserChoice()
}

/**
 * Reasons why a user might override the recommendation.
 * These are not judgments - user autonomy is always respected.
 */
sealed class OverrideReason {
    /**
     * User wants to try something new/different.
     */
    data object Curiosity : OverrideReason()

    /**
     * User knows something the system doesn't about partner preferences.
     */
    data class PartnerInsight(val note: String? = null) : OverrideReason()

    /**
     * Special occasion calls for a specific choice.
     */
    data class SpecialOccasion(val occasion: String) : OverrideReason()

    /**
     * User simply prefers this option.
     */
    data object PersonalPreference : OverrideReason()

    /**
     * External constraints (availability, dietary, budget, etc.)
     */
    data class ExternalConstraint(val constraint: String) : OverrideReason()

    /**
     * No specific reason provided (still valid!).
     */
    data object Unspecified : OverrideReason()
}

/**
 * Checks if override is allowed. Always returns true.
 * This embodies the ethical principle that users can ALWAYS override.
 */
fun canOverride(): Boolean = true

/**
 * Extension to check if this choice was an override.
 */
fun UserChoice.isOverride(): Boolean = this is UserChoice.ManualOverride

/**
 * Extension to check if this choice accepted the recommendation.
 */
fun UserChoice.isAcceptance(): Boolean = this is UserChoice.AcceptRecommendation

/**
 * Extension to get the final cake that was chosen.
 */
fun UserChoice.chosenCakeId(): String = when (this) {
    is UserChoice.AcceptRecommendation -> recommendedCake.cakeId
    is UserChoice.ManualOverride -> chosenCakeId
}
