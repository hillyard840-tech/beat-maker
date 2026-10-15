package com.example.data

import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val projectDao: ProjectDao
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
        projectDao.deleteProject(project)
    }
}
