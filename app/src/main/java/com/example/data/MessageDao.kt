package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE (:projectId IS NULL AND projectId IS NULL) OR projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesByProject(projectId: Int?): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE (:projectId IS NULL AND projectId IS NULL) OR projectId = :projectId")
    suspend fun clearHistoryForProject(projectId: Int?)

    @Query("DELETE FROM messages")
    suspend fun clearHistory()

    @Query("UPDATE messages SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, isBookmarked: Boolean)

    @Query("SELECT * FROM messages WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedMessages(): Flow<List<Message>>

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Int)
}
