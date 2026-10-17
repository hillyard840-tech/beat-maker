package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Message::class, Skill::class, MemoryItem::class, Project::class, Conversation::class,
        AgentAction::class, Reminder::class, ScheduledTask::class, SavedOpportunity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun skillDao(): SkillDao
    abstract fun memoryDao(): MemoryDao
    abstract fun projectDao(): ProjectDao
    abstract fun conversationDao(): ConversationDao
    abstract fun agentActionDao(): AgentActionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao
    abstract fun savedOpportunityDao(): SavedOpportunityDao

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

        /** v6 adds phone action cards, reminders and scheduled tasks. Nothing existing changes. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `agent_actions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`conversationId` INTEGER, " +
                        "`tool` TEXT NOT NULL, " +
                        "`argsJson` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`details` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`resultNote` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`triggerAt` INTEGER NOT NULL, " +
                        "`conversationId` INTEGER, " +
                        "`isDone` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scheduled_tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`prompt` TEXT NOT NULL, " +
                        "`hour` INTEGER NOT NULL, " +
                        "`minute` INTEGER NOT NULL, " +
                        "`repeat` TEXT NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, " +
                        "`useWeb` INTEGER NOT NULL, " +
                        "`conversationId` INTEGER, " +
                        "`nextRunAt` INTEGER, " +
                        "`lastRunAt` INTEGER, " +
                        "`lastResult` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        /** v7 adds saved jobs and ideas from Opportunities. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `opportunities` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`subtitle` TEXT NOT NULL, " +
                        "`url` TEXT, " +
                        "`details` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Single shared instance. Opening the same database file through several Room instances
         * (e.g. ViewModel + background workers) breaks Flow invalidation, so writes from the worker
         * would never show up in the UI until restart.
         */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    // Versions before 4 never shipped schemas we can migrate from.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build()
                    .also { instance = it }
            }
    }
}
