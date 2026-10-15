package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Message::class, Skill::class, MemoryItem::class, Project::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun skillDao(): SkillDao
    abstract fun memoryDao(): MemoryDao
    abstract fun projectDao(): ProjectDao
}
