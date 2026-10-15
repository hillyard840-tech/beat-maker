package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Message::class, Skill::class, MemoryItem::class, Project::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun skillDao(): SkillDao
    abstract fun memoryDao(): MemoryDao
    abstract fun projectDao(): ProjectDao

    companion object {
        private const val DATABASE_NAME = "chat-database"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Single shared instance. Opening the same database file through several Room instances
         * (e.g. ViewModel + LearningWorker) breaks Flow invalidation, so writes from the worker
         * would never show up in the UI until restart.
         */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
