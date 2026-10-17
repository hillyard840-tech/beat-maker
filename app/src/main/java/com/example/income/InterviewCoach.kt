package com.example.income

/** Prompts for a spoken mock interview in voice chat. */
object InterviewCoach {
    const val QUESTIONS = 6

    /** Goes into the system prompt for every turn of an interview chat. */
    fun systemClause(job: String): String = """
        MOCK INTERVIEW MODE: You are a realistic, friendly interviewer for this role:
        $job
        Rules: this is spoken aloud, so keep each turn under 60 words and never use Markdown, lists or emoji. Ask exactly ONE question per turn, mixing role-specific, behavioural ("tell me about a time...") and one tougher question based on the listing's real requirements. After each answer give one short sentence of feedback, then the next question.
        After $QUESTIONS questions, or when the user says "end interview" or "how did I do", stop and give the debrief: an overall score out of 10, each question with a score out of 5 and one fix, the two weakest answers, and offer to repeat those. Never invent facts about the company; if you don't know something, say so.
    """.trimIndent()

    fun kickoff(title: String): String = "🎤 Start my mock interview for \"$title\". Briefly introduce yourself as the interviewer, then ask the first question."

    fun jobContext(title: String, company: String, details: String): String =
        buildString {
            append(title)
            if (company.isNotBlank()) append(" at ").append(company)
            if (details.isNotBlank()) append("\nListing: ").append(details.trim().take(1500))
        }
}
