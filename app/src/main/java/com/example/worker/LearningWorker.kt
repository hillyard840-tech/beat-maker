package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.Room
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearningWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "chat-database"
            ).fallbackToDestructiveMigration().build()
            
            val dao = db.messageDao()
            val skillDao = db.skillDao()

            val prefs = applicationContext.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)
            val isPaused = prefs.getBoolean("background_tasks_paused", false)
            if (isPaused) {
                return@withContext Result.success()
            }

            val customKey = prefs.getString("custom_api_key", "")?.trim() ?: ""
            val apiKey = if (customKey.isNotEmpty()) customKey else BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty()) return@withContext Result.success()

            val topics = listOf(
                "Python & Memory Mgmt",
                "Zero Trust & Cryptography",
                "Autonomous Agent Engineering",
                "Rust Low-Level Programming",
                "MLOps & CI/CD",
                "Arbitrary Code Sandboxing",
                "Web App Secure Coding"
            )
            val topic = topics.random()

            val prompt = "Autonomous Learning summary for: $topic. Provide 1 practical insight in under 70 words."

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user"))
            )

            // Strictly use ultra-lightweight models for background tasks to conserve user quota
            var text: String? = null
            val learningModels = listOf("gemini-3.5-flash-lite", "gemini-flash-lite-latest")
            for (model in learningModels) {
                try {
                    val response = RetrofitClient.service.generateContent(model, apiKey, request)
                    text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (text != null) break
                } catch (_: Exception) {
                    continue
                }
            }

            if (text != null) {
                // Update chat
                val message = Message(role = "model", text = "🧠 **Autonomous Learning Update [$topic]:**\n\n$text")
                dao.insertMessage(message)
                
                // Update skill
                val currentSkill = skillDao.getSkillByTopic(topic)
                val newProgress = ((currentSkill?.progress ?: 0) + (10..25).random()).coerceAtMost(100)
                
                val level = when {
                    newProgress < 30 -> "Beginner"
                    newProgress < 60 -> "Intermediate"
                    newProgress < 90 -> "Advanced"
                    else -> "Expert"
                }
                
                skillDao.insertOrUpdateSkill(
                    Skill(
                        topic = topic,
                        progress = newProgress,
                        level = level,
                        lastInsight = text.take(150) + "...",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Do NOT call Result.retry() as it causes rapid retry storms that trigger 429 quota locks
            Result.success()
        }
    }
}
