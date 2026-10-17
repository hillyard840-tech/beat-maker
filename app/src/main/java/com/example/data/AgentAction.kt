package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Something Kora offered to do on the phone (set an alarm, draft a text, add an event...).
 * It shows in the chat as a card and only runs when the user taps Confirm.
 */
@Entity(tableName = "agent_actions")
data class AgentAction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: Int? = null,
    val tool: String,
    /** The tool arguments as a JSON object, exactly as the model sent them. */
    val argsJson: String,
    val title: String,
    val details: String = "",
    val status: String = STATUS_PENDING,
    val resultNote: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DONE = "done"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_FAILED = "failed"
    }
}
