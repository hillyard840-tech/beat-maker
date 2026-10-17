package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY hour ASC, minute ASC")
    fun all(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks ORDER BY hour ASC, minute ASC")
    suspend fun allOnce(): List<ScheduledTask>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ScheduledTask?

    @Insert
    suspend fun insert(task: ScheduledTask): Long

    @Update
    suspend fun update(task: ScheduledTask)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun delete(id: Int)
}
