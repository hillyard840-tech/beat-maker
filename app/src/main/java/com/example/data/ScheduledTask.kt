package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A prompt Kora runs on its own at a set time (e.g. a morning briefing). The answer is saved in
 * its own chat and announced with a notification.
 */
@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val prompt: String,
    val hour: Int,
    val minute: Int,
    /** One of [REPEAT_DAILY], [REPEAT_WEEKDAYS], [REPEAT_SUNDAYS], [REPEAT_ONCE]. */
    val repeat: String = REPEAT_DAILY,
    val enabled: Boolean = true,
    /** Whether the task may search and read the web. */
    val useWeb: Boolean = true,
    val conversationId: Int? = null,
    val nextRunAt: Long? = null,
    val lastRunAt: Long? = null,
    val lastResult: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val REPEAT_DAILY = "daily"
        const val REPEAT_WEEKDAYS = "weekdays"
        const val REPEAT_ONCE = "once"
        /** Weekly, on Sunday. */
        const val REPEAT_SUNDAYS = "sundays"
    }
}
