package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isDone ASC, triggerAt ASC")
    fun all(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isDone = 0 ORDER BY triggerAt ASC")
    suspend fun pendingOnce(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Int)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM reminders WHERE isDone = 1")
    suspend fun clearDone()
}
