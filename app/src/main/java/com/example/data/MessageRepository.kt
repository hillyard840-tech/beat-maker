package com.example.data

import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val projectDao: ProjectDao,
    private val conversationDao: ConversationDao,
    private val agentActionDao: AgentActionDao
) {
    val allMessages: Flow<List<Message>> = messageDao.getAllMessages()
    val allMemories: Flow<List<MemoryItem>> = memoryDao.getAllMemories()
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    fun getMessagesForProject(projectId: Int?): Flow<List<Message>> =
        messageDao.getMessagesByProject(projectId)

    suspend fun insert(message: Message) = messageDao.insertMessage(message)
    suspend fun updateBookmark(id: Int, isBookmarked: Boolean) = messageDao.updateBookmark(id, isBookmarked)
    val bookmarkedMessages: Flow<List<Message>> = messageDao.getBookmarkedMessages()
    suspend fun clear() = messageDao.clearHistory()
    suspend fun clearForProject(projectId: Int?) = messageDao.clearHistoryForProject(projectId)
    suspend fun deleteMessage(id: Int) = messageDao.deleteMessage(id)

    // Memories
    suspend fun insertMemory(memory: MemoryItem) = memoryDao.insertMemory(memory)
    suspend fun updateMemory(memory: MemoryItem) = memoryDao.updateMemory(memory)
    suspend fun deleteMemory(memory: MemoryItem) = memoryDao.deleteMemory(memory)
    suspend fun deleteMemoryById(id: Int) = memoryDao.deleteById(id)
    suspend fun clearAllMemories() = memoryDao.clearAll()

    // Projects
    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)
    suspend fun updateProject(project: Project) = projectDao.updateProject(project)
    suspend fun deleteProject(project: Project) {
        messageDao.clearHistoryForProject(project.id)
        conversationDao.deleteForProject(project.id)
        projectDao.deleteProject(project)
        agentActionDao.deleteOrphans()
    }

    suspend fun imagePathsForProject(projectId: Int): List<String> = messageDao.getImagePathsForProject(projectId)

    // Conversations
    fun conversationsForProject(projectId: Int?): Flow<List<Conversation>> = conversationDao.getForProject(projectId)
    fun messagesForConversation(conversationId: Int): Flow<List<Message>> = messageDao.getMessagesByConversation(conversationId)
    suspend fun latestConversation(projectId: Int?): Conversation? = conversationDao.getLatestForProject(projectId)
    suspend fun getConversation(id: Int): Conversation? = conversationDao.getById(id)
    suspend fun createConversation(conversation: Conversation): Int = conversationDao.insert(conversation).toInt()
    suspend fun renameConversation(id: Int, title: String) = conversationDao.rename(id, title)
    suspend fun touchConversation(id: Int) = conversationDao.touch(id, System.currentTimeMillis())
    suspend fun imagePathsForConversation(id: Int): List<String> = messageDao.getImagePathsForConversation(id)
    suspend fun messagesOnce(conversationId: Int): List<Message> = messageDao.getMessagesForConversationOnce(conversationId)

    // Phone action cards
    fun actionsForConversation(conversationId: Int): Flow<List<AgentAction>> = agentActionDao.forConversation(conversationId)
    suspend fun actionsOnce(conversationId: Int): List<AgentAction> = agentActionDao.forConversationOnce(conversationId)
    suspend fun insertAction(action: AgentAction): Int = agentActionDao.insert(action).toInt()
    suspend fun getAction(id: Int): AgentAction? = agentActionDao.getById(id)
    suspend fun updateActionStatus(id: Int, status: String, note: String?) = agentActionDao.updateStatus(id, status, note)

    /** Deletes a conversation and all of its messages. */
    suspend fun deleteConversation(id: Int) {
        messageDao.clearConversation(id)
        agentActionDao.deleteForConversation(id)
        conversationDao.delete(id)
    }
}
