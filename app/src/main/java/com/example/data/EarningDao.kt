package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EarningDao {
    @Query("SELECT * FROM earnings ORDER BY earnedAt DESC")
    fun all(): Flow<List<Earning>>

    @Query("SELECT * FROM earnings ORDER BY earnedAt DESC")
    suspend fun allOnce(): List<Earning>

    @Insert
    suspend fun insert(earning: Earning): Long

    @Query("DELETE FROM earnings WHERE id = :id")
    suspend fun delete(id: Int)
}
