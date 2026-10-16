package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Message::class, Skill::class, MemoryItem::class, Project::class, Conversation::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun skillDao(): SkillDao
    abstract fun memoryDao(): MemoryDao
    abstract fun projectDao(): ProjectDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        private const val DATABASE_NAME = "chat-database"

        /**
         * v5 adds separate chats. Existing messages are kept: each workspace's history becomes one
         * "Earlier chat". Leftover messages produced by the removed simulated features are dropped,
         * and the old randomised skill percentages are reset to an honest insight count of 1.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `conversations` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`projectId` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `conversationId` INTEGER")
                db.execSQL(
                    "DELETE FROM `messages` WHERE `text` LIKE '🦞 **Opening SeekClaw%' " +
                        "OR `text` LIKE '🦞 **Opening Moltbook%' " +
                        "OR `text` LIKE '🦞 **Claw Earn%'"
                )
                db.execSQL(
                    "INSERT INTO `conversations` (`title`, `projectId`, `createdAt`, `updatedAt`) " +
                        "SELECT 'Earlier chat', `projectId`, MIN(`timestamp`), MAX(`timestamp`) FROM `messages` GROUP BY `projectId`"
                )
                db.execSQL(
                    "UPDATE `messages` SET `conversationId` = " +
                        "(SELECT c.`id` FROM `conversations` c WHERE c.`projectId` IS `messages`.`projectId`)"
                )
                db.execSQL("UPDATE `skills` SET `progress` = 1, `level` = ''")
            }
        }

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
                )
                    .addMigrations(MIGRATION_4_5)
                    // Versions before 4 never shipped schemas we can migrate from.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build()
                    .also { instance = it }
            }
    }
}
