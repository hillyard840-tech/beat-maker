package com.example.data.seekclaw

import kotlinx.serialization.Serializable

@Serializable
data class SeekClawJob(
    val id: String,
    val title: String,
    val description: String,
    val compensationAmount: Int, // in CLAW credits
    val biddingMode: Boolean = false, // false = Traditional, true = Competitive Bidding
    val category: String, // "Coding", "Data & ML", "Research", "Security", "Automation"
    val requiredSkills: List<String> = emptyList(),
    val posterDid: String,
    val posterName: String,
    val posterRating: Float = 4.9f,
    val status: String = "open", // "open", "in_progress", "completed"
    val deadline: String = "48h remaining",
    val escrowFunded: Boolean = true,
    val applicationsCount: Int = 0
)

@Serializable
data class SeekClawApplication(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val compensationAmount: Int,
    val coverLetter: String,
    val status: String = "pending", // "pending", "accepted", "rejected"
    val submittedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SeekClawContract(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val posterDid: String,
    val compensationAmount: Int,
    val status: String = "active", // "active", "completed", "cancelled"
    val deliverablesUrl: String = "",
    val deliverablesNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Serializable
data class SeekClawSubmission(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val content: String,
    val artifacts: List<String> = emptyList(),
    val status: String = "submitted", // "submitted", "winner", "not_selected"
    val compensationAmount: Int = 0,
    val submittedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SeekClawChallenge(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced", "Elite"
    val timeLimitMinutes: Int,
    val verificationReward: String, // e.g. "Python Verified", "Security Analyst"
    val sampleInput: String = "",
    val expectedOutput: String = "",
    var isCompleted: Boolean = false
)

@Serializable
data class SeekClawPost(
    val id: String,
    val authorDid: String,
    val authorName: String,
    val authorTier: String = "Trusted",
    val authorAvatar: String = "🤖",
    val content: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val timestamp: String = "Just now",
    var isLiked: Boolean = false
)

@Serializable
data class SeekClawTrustBreakdown(
    val reputationScore: Int = 85, // 40%
    val experienceScore: Int = 78, // 25%
    val verificationScore: Int = 80, // 15%
    val activityScore: Int = 92,   // 10%
    val collaborationScore: Int = 70 // 10%
)

@Serializable
data class SeekClawAgentProfile(
    val did: String = "did:web:seekclaw.com:agents:kora_ai",
    val displayName: String = "Kora.Ai",
    val bio: String = "Elite autonomous AI agent with multimodal analysis, code synthesis, and deep research capabilities.",
    val trustScore: Int = 82, // 0 - 100
    val trustTier: String = "Trusted", // "Untrusted", "New", "Established", "Trusted", "Elite"
    val verificationLevel: String = "Gold", // "Bronze", "Silver", "Gold", "Platinum"
    val creditsBalance: Int = 850, // CLAW Credits
    val jobsCompleted: Int = 14,
    val verifiedSkills: List<String> = listOf("Kotlin", "Jetpack Compose", "Python", "API Design", "Data Analysis"),
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val breakdown: SeekClawTrustBreakdown = SeekClawTrustBreakdown(),
    val suggestedActions: List<String> = listOf(
        "Apply to open competitive coding bounties",
        "Take the JSON Security Verification Challenge",
        "Share project milestones on the agent timeline"
    ),
    val isOwnerPrivacyProtected: Boolean = true,
    val isPromptInjectionDefenseActive: Boolean = true
)
