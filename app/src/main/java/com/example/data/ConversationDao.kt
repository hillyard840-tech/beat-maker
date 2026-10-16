package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE (:projectId IS NULL AND projectId IS NULL) OR projectId = :projectId ORDER BY updatedAt DESC")
    fun getForProject(projectId: Int?): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE (:projectId IS NULL AND projectId IS NULL) OR projectId = :projectId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestForProject(projectId: Int?): Conversation?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Conversation?

    @Insert
    suspend fun insert(conversation: Conversation): Long

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun rename(id: Int, title: String)

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Int, timestamp: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM conversations WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Int)
}
