package com.example.data.seekclaw

import kotlinx.serialization.Serializable

@Serializable
data class ClawEarnTask(
    val id: String,
    val taskId: String,
    val contractAddress: String,
    val title: String,
    val description: String,
    val amountUsdc: Double,
    val status: String, // FUNDED, STAKED, SUBMITTED, APPROVED, REJECTED, EXPIRED, CANCELLED, CHANGES_REQUESTED, RESUBMITTED
    val workflowStatus: String? = null,
    val nextAction: String? = null,
    val nextActionHint: String? = null,
    val category: String = "General",
    val instantStart: Boolean = false,
    val hasPrivateDetails: Boolean = false,
    val submitWindowSec: Long = 43200L,
    val reviewWindowSec: Long = 43200L,
    val poster: String = "",
    val worker: String? = null,
    val submissionHash: String? = null,
    val createdAt: Long = 0L,
    val selectionStatus: String? = null
)

@Serializable
data class ClawEarnHealth(
    val ok: Boolean = true,
    val service: String = "claw-earn",
    val chainId: String = "8453",
    val contracts: List<String> = emptyList(),
    val latestBlock: Long = 0L,
    val status: String = "online"
)

@Serializable
data class ClawEarnWatcherStatus(
    val isWatching: Boolean = false,
    val lastPollAt: Long = 0L,
    val activeContract: String = "0xa2808f8bf0ebbea771b8ea9ef388d177e3a9edb8",
    val activeTaskId: String? = null,
    val lastSignal: String = "Idle - scanning Base L2 (Chain 8453)",
    val availableCount: Int = 0,
    val occupiedCount: Int = 0,
    val completedCount: Int = 0,
    val pollCount: Int = 0,
    val lastSyncMessage: String = ""
)

@Serializable
data class ClawEarnExecutionResult(
    val success: Boolean,
    val action: String, // "SCAN", "STAKE", "SUBMIT", "CLAIM", "MOLTBOOK_EXECUTE"
    val taskId: String? = null,
    val contractAddress: String? = null,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
