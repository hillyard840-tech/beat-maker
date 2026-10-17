package com.example.income

import android.content.SharedPreferences

/** What Kora knows about the user's work goals. Stored on the phone only. */
data class IncomeProfile(
    val skills: String = "",
    val interests: String = "",
    val hoursPerWeek: Int = 10,
    val budget: String = "",
    val goal: String = GOAL_SIDE,
    val location: String = ""
) {
    val isEmpty: Boolean get() = skills.isBlank() && interests.isBlank()

    /** Search words for job boards: the user's skills, or interests if no skills are set. */
    val jobQuery: String get() = skills.ifBlank { interests }.split(',').map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()

    fun promptBlock(): String = if (isEmpty) "" else buildString {
        appendLine("USER'S WORK PROFILE:")
        if (skills.isNotBlank()) appendLine("• Skills: $skills")
        if (interests.isNotBlank()) appendLine("• Interests: $interests")
        appendLine("• Time available: about $hoursPerWeek hours a week")
        if (budget.isNotBlank()) appendLine("• Money they can put in: $budget")
        appendLine("• Goal: ${goalLabel(goal)}")
        if (location.isNotBlank()) appendLine("• Based in: $location")
    }

    fun save(prefs: SharedPreferences) {
        prefs.edit()
            .putString(K_SKILLS, skills.trim())
            .putString(K_INTERESTS, interests.trim())
            .putInt(K_HOURS, hoursPerWeek)
            .putString(K_BUDGET, budget.trim())
            .putString(K_GOAL, goal)
            .putString(K_LOCATION, location.trim())
            .apply()
    }

    companion object {
        const val GOAL_SIDE = "side"
        const val GOAL_JOB = "job"
        const val GOAL_BUSINESS = "business"
        private const val K_SKILLS = "income_skills"
        private const val K_INTERESTS = "income_interests"
        private const val K_HOURS = "income_hours"
        private const val K_BUDGET = "income_budget"
        private const val K_GOAL = "income_goal"
        private const val K_LOCATION = "income_location"

        fun goalLabel(goal: String) = when (goal) {
            GOAL_JOB -> "a new job"
            GOAL_BUSINESS -> "build a product or small business"
            else -> "extra income on the side"
        }

        fun load(prefs: SharedPreferences) = IncomeProfile(
            skills = prefs.getString(K_SKILLS, "").orEmpty(),
            interests = prefs.getString(K_INTERESTS, "").orEmpty(),
            hoursPerWeek = prefs.getInt(K_HOURS, 10),
            budget = prefs.getString(K_BUDGET, "").orEmpty(),
            goal = prefs.getString(K_GOAL, GOAL_SIDE) ?: GOAL_SIDE,
            location = prefs.getString(K_LOCATION, "").orEmpty()
        )
    }
}
