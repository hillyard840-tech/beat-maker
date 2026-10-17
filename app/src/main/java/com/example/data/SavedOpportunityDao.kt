package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedOpportunityDao {
    @Query("SELECT * FROM opportunities ORDER BY updatedAt DESC")
    fun all(): Flow<List<SavedOpportunity>>

    @Query("SELECT * FROM opportunities ORDER BY updatedAt DESC")
    suspend fun allOnce(): List<SavedOpportunity>

    @Query("SELECT COUNT(*) FROM opportunities WHERE kind = :kind AND title = :title AND ((:url IS NULL AND url IS NULL) OR url = :url)")
    suspend fun countSame(kind: String, title: String, url: String?): Int

    @Insert
    suspend fun insert(item: SavedOpportunity): Long

    @Query("UPDATE opportunities SET status = :status, updatedAt = :at WHERE id = :id")
    suspend fun setStatus(id: Int, status: String, at: Long)

    @Query("DELETE FROM opportunities WHERE id = :id")
    suspend fun delete(id: Int)
}
