package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String,
    val text: String,
    val imagePath: String? = null,
    val projectId: Int? = null,
    val conversationId: Int? = null,
    val fileAttachmentName: String? = null,
    val fileAttachmentType: String? = null,
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
