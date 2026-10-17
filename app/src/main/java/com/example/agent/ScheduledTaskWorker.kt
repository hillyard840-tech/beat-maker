package com.example.agent

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.api.LlmSettings
import com.example.data.AppDatabase
import com.example.data.Conversation
import com.example.data.Message
import com.example.data.ScheduledTask
import com.example.util.AgentTime
import com.example.util.SpeechText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs one scheduled task, saves the answer in the task's chat and posts a notification. */
class ScheduledTaskWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val id = inputData.getInt(TaskScheduler.INPUT_ID, -1)
        val scheduled = inputData.getBoolean(TaskScheduler.INPUT_SCHEDULED, false)
        val db = AppDatabase.getInstance(context)
        val dao = db.scheduledTaskDao()
        var task = dao.getById(id) ?: return@withContext Result.success()

        if (scheduled) {
            if (!task.enabled) return@withContext Result.success()
            // Arm the next run first, so one failed run never stops a daily task.
            task = if (task.repeat == ScheduledTask.REPEAT_ONCE) {
                task.copy(enabled = false, nextRunAt = null).also { dao.update(it); TaskScheduler.disarm(context, it.id) }
            } else {
                val saved = task
                TaskScheduler.save(context, saved)
                dao.getById(id) ?: saved
            }
        }

        val prefs = context.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)
        if (scheduled && prefs.getBoolean("background_tasks_paused", false)) {
            dao.update(task.copy(lastResult = "Skipped: background tasks are paused."))
            return@withContext Result.success()
        }
        val settings = LlmSettings.fromPrefs(prefs)
        if (!settings.isReady) {
            dao.update(task.copy(lastRunAt = System.currentTimeMillis(), lastResult = "Not run: no API key or server is set up."))
            return@withContext Result.success()
        }

        val answer = try {
            AgentRunner.run(
                context = context,
                settings = settings,
                geminiModel = prefs.getString("selected_model", null).orEmpty(),
                instruction = task.prompt,
                useWeb = task.useWeb,
                memories = if (prefs.getBoolean("memory_enabled", true)) db.memoryDao().allOnce().map { it.fact } else emptyList(),
                profile = com.example.income.IncomeProfile.load(prefs).promptBlock()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val reason = e.message ?: e.javaClass.simpleName
            dao.update(task.copy(lastRunAt = System.currentTimeMillis(), lastResult = "Failed: ${reason.take(200)}"))
            Notifications.post(context, 300_000 + task.id, Notifications.CHANNEL_TASKS, task.title, "Couldn't run this task: ${reason.take(160)}", task.conversationId)
            // No automatic retry: retries burn quota and the next scheduled run comes anyway.
            return@withContext Result.success()
        }

        val now = System.currentTimeMillis()
        val conversations = db.conversationDao()
        val chatId = task.conversationId?.takeIf { conversations.getById(it) != null }
            ?: conversations.insert(Conversation(title = task.title)).toInt()
        db.messageDao().insertMessage(
            Message(role = "model", text = "**⏰ ${task.title}** · ${AgentTime.label(now)}\n\n$answer", conversationId = chatId, timestamp = now)
        )
        conversations.touch(chatId, now)
        val plain = SpeechText.clean(answer)
        dao.update(task.copy(conversationId = chatId, lastRunAt = now, lastResult = plain.take(240)))
        Notifications.post(context, 300_000 + task.id, Notifications.CHANNEL_TASKS, task.title, plain.take(600), chatId)
        Result.success()
    }
}
