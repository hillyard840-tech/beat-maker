package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentActionDao {
    @Query("SELECT * FROM agent_actions WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun forConversation(conversationId: Int): Flow<List<AgentAction>>

    @Query("SELECT * FROM agent_actions WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun forConversationOnce(conversationId: Int): List<AgentAction>

    @Query("SELECT * FROM agent_actions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): AgentAction?

    @Insert
    suspend fun insert(action: AgentAction): Long

    @Query("UPDATE agent_actions SET status = :status, resultNote = :note WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, note: String?)

    @Query("DELETE FROM agent_actions WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Int)

    /** Removes cards whose chat no longer exists (e.g. after a workspace was deleted). */
    @Query("DELETE FROM agent_actions WHERE conversationId IS NULL OR conversationId NOT IN (SELECT id FROM conversations)")
    suspend fun deleteOrphans()
}
