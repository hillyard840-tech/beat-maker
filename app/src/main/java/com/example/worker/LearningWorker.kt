package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearningWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val skillDao = db.skillDao()

            val prefs = applicationContext.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)
            val isPaused = prefs.getBoolean("background_tasks_paused", false)
            if (isPaused) {
                return@withContext Result.success()
            }

            val settings = LlmSettings.fromPrefs(prefs)
            if (!settings.isReady) return@withContext Result.success()

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
            // OpenAI-compatible servers use their own configured model, so one attempt is enough.
            val learningModels = if (settings.provider == LlmProvider.GEMINI) ModelRegistry.ALIAS_FALLBACKS else listOf("")
            for (model in learningModels) {
                try {
                    val response = LlmGateway.generate(settings, model, request)
                    text = response.candidates.firstOrNull()?.content?.parts
                        ?.firstOrNull { it.thought != true && !it.text.isNullOrBlank() }?.text
                    if (text != null) break
                } catch (_: Exception) {
                    continue
                }
            }

            if (text != null) {
                // Store the real insight on the dashboard. "progress" counts insights collected;
                // nothing here is invented or randomised.
                val currentSkill = skillDao.getSkillByTopic(topic)
                skillDao.insertOrUpdateSkill(
                    Skill(
                        topic = topic,
                        progress = (currentSkill?.progress ?: 0) + 1,
                        level = "",
                        lastInsight = text.trim(),
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
