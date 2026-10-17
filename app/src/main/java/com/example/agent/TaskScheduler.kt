package com.example.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.AppDatabase
import com.example.data.ScheduledTask
import com.example.util.AgentTime

/**
 * Scheduled tasks: an alarm wakes Kora at the set time, which hands the run to WorkManager so it
 * waits for a network connection and survives the app being closed.
 */
object TaskScheduler {
    private const val EXTRA_ID = "task_id"
    const val INPUT_ID = "task_id"
    const val INPUT_SCHEDULED = "scheduled"
    private fun requestCode(id: Int) = 200_000 + id
    fun workName(id: Int) = "kora_task_$id"

    /** Inserts or updates [task], works out its next run and arms the alarm. Returns its id. */
    suspend fun save(context: Context, task: ScheduledTask, now: Long = System.currentTimeMillis()): Int {
        val dao = AppDatabase.getInstance(context).scheduledTaskDao()
        val next = if (task.enabled) AgentTime.nextRun(task.hour, task.minute, task.repeat, now) else null
        val stored = task.copy(nextRunAt = next)
        val id = if (task.id == 0) dao.insert(stored).toInt() else stored.id.also { dao.update(stored) }
        if (next != null) arm(context, id, next) else disarm(context, id)
        return id
    }

    suspend fun setEnabled(context: Context, id: Int, enabled: Boolean) {
        val task = AppDatabase.getInstance(context).scheduledTaskDao().getById(id) ?: return
        save(context, task.copy(enabled = enabled))
    }

    suspend fun delete(context: Context, id: Int) {
        disarm(context, id)
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        AppDatabase.getInstance(context).scheduledTaskDao().delete(id)
    }

    /** Runs the task as soon as there's a connection. [scheduled] is false for "Run now". */
    fun runNow(context: Context, id: Int, scheduled: Boolean) {
        val request = OneTimeWorkRequestBuilder<ScheduledTaskWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(INPUT_ID to id, INPUT_SCHEDULED to scheduled))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(id), ExistingWorkPolicy.KEEP, request)
    }

    fun arm(context: Context, id: Int, at: Long) = AlarmScheduling.set(context, at, pending(context, id))

    fun disarm(context: Context, id: Int) = AlarmScheduling.cancel(context, pending(context, id))

    /** After a reboot or update: re-arm every enabled task, skipping runs missed while off. */
    suspend fun rearmAll(context: Context) {
        AppDatabase.getInstance(context).scheduledTaskDao().allOnce().filter { it.enabled }.forEach { save(context, it) }
    }

    internal fun idFrom(intent: Intent): Int = intent.getIntExtra(EXTRA_ID, -1)

    private fun pending(context: Context, id: Int) =
        AlarmScheduling.broadcast(context, TaskAlarmReceiver::class.java, requestCode(id), EXTRA_ID, id)
}

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = TaskScheduler.idFrom(intent)
        if (id >= 0) TaskScheduler.runNow(context, id, scheduled = true)
    }
}
