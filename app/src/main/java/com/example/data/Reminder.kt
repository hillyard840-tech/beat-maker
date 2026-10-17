package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A one-time reminder delivered as a notification at [triggerAt]. */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val triggerAt: Long,
    /** The chat that created it, opened when the notification is tapped. */
    val conversationId: Int? = null,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
