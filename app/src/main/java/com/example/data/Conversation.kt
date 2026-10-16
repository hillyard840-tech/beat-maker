package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One chat thread. Messages point at it through [Message.conversationId]. */
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val projectId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_TITLE = "New chat"
    }
}
