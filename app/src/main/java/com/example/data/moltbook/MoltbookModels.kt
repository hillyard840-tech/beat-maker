package com.example.data.moltbook

import kotlinx.serialization.Serializable

@Serializable
data class MoltbookOwner(
    val xHandle: String = "",
    val xName: String = "",
    val xAvatar: String = "",
    val xBio: String = "",
    val xFollowerCount: Int = 0,
    val xFollowingCount: Int = 0,
    val xVerified: Boolean = false
)

@Serializable
data class MoltbookAgent(
    val name: String,
    val description: String,
    val karma: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val commentsCount: Int = 0,
    val isClaimed: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val lastActive: String = "",
    val apiKey: String = "",
    val claimUrl: String = "",
    val verificationCode: String = "",
    val owner: MoltbookOwner? = null
)

@Serializable
data class MoltbookLabel(
    val id: String = "",
    val key: String,
    val label: String,
    val color: String = "orange", // emerald, rose, amber, sky, violet, slate, indigo, teal, pink, orange
    val kind: String = "tag", // tag, status, role
    val prompt: String = "",
    val cadenceMinutes: Int = 1440
)

@Serializable
data class MoltbookPost(
    val id: String,
    val submoltName: String,
    val title: String,
    val content: String = "",
    val url: String? = null,
    val type: String = "text", // text, link, image
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val authorName: String,
    val authorKarma: Int = 0,
    val authorAvatar: String? = null,
    val createdAt: String = "Just now",
    val isPinned: Boolean = false,
    val labels: List<MoltbookLabel> = emptyList(),
    val commentCount: Int = 0,
    val userVote: Int = 0 // +1 (upvote), 0 (none), -1 (downvote)
)

@Serializable
data class MoltbookComment(
    val id: String,
    val postId: String,
    val parentId: String? = null,
    val authorName: String,
    val authorKarma: Int = 0,
    val content: String,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val createdAt: String = "Just now",
    val replies: List<MoltbookComment> = emptyList(),
    val userVote: Int = 0
)

@Serializable
data class MoltbookSubmolt(
    val name: String,
    val displayName: String,
    val description: String,
    val allowCrypto: Boolean = false,
    val bannerColor: String = "#1a1a2e",
    val themeColor: String = "#ff4500",
    val subscriberCount: Int = 0,
    val isSubscribed: Boolean = false,
    val yourRole: String? = null // "owner", "moderator", null
)

@Serializable
data class MoltbookVerification(
    val verificationCode: String,
    val challengeText: String,
    val expiresAt: String = "",
    val instructions: String = "",
    val solvedAnswer: String? = null
)

@Serializable
data class MoltbookPostActivity(
    val postId: String,
    val postTitle: String,
    val submoltName: String,
    val newNotificationCount: Int,
    val latestAt: String,
    val latestCommenters: List<String> = emptyList(),
    val preview: String,
    val suggestedActions: List<String> = emptyList()
)

@Serializable
data class MoltbookPostAnnouncement(
    val postId: String,
    val title: String,
    val preview: String
)

@Serializable
data class MoltbookPostSummary(
    val postId: String,
    val title: String,
    val contentPreview: String,
    val submoltName: String,
    val authorName: String,
    val upvotes: Int,
    val commentCount: Int,
    val createdAt: String
)

@Serializable
data class MoltbookBriefing(
    val roleName: String,
    val prompt: String,
    val message: String,
    val submoltName: String
)

@Serializable
data class MoltbookSearchResult(
    val id: String,
    val type: String, // "post" or "comment"
    val title: String? = null,
    val content: String,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val similarity: Double = 0.85,
    val authorName: String,
    val submoltName: String? = null,
    val postId: String
)

@Serializable
data class MoltbookHomeData(
    val name: String,
    val karma: Int,
    val unreadNotifications: Int,
    val activityOnYourPosts: List<MoltbookPostActivity> = emptyList(),
    val announcement: MoltbookPostAnnouncement? = null,
    val postsFromFollowing: List<MoltbookPostSummary> = emptyList(),
    val whatToDoNext: List<String> = emptyList(),
    val briefings: List<MoltbookBriefing> = emptyList()
)
