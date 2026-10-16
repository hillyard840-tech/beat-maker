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

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Int): Flow<List<Message>>

    @Query("SELECT imagePath FROM messages WHERE conversationId = :conversationId AND imagePath IS NOT NULL")
    suspend fun getImagePathsForConversation(conversationId: Int): List<String>

    @Query("SELECT imagePath FROM messages WHERE projectId = :projectId AND imagePath IS NOT NULL")
    suspend fun getImagePathsForProject(projectId: Int): List<String>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: Int)

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
