package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions ORDER BY createdAt DESC")
    fun all(): Flow<List<Prediction>>

    @Query("SELECT * FROM predictions ORDER BY createdAt DESC")
    suspend fun allOnce(): List<Prediction>

    @Query("SELECT * FROM predictions WHERE happened IS NULL")
    suspend fun unresolved(): List<Prediction>

    @Insert
    suspend fun insert(prediction: Prediction): Long

    @Query("UPDATE predictions SET happened = :happened, resolvedAt = :at WHERE id = :id")
    suspend fun resolve(id: Int, happened: Boolean, at: Long)

    @Query("DELETE FROM predictions WHERE id = :id")
    suspend fun delete(id: Int)
}
