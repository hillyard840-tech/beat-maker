package com.example.income

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentRunner
import com.example.agent.TaskScheduler
import com.example.api.LlmSettings
import com.example.data.AppDatabase
import com.example.data.Earning
import com.example.data.SavedOpportunity
import com.example.data.ScheduledTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the Opportunities screen: live jobs, income ideas, build ideas and saved items. */
class OpportunitiesViewModel(application: Application) : AndroidViewModel(application) {
    sealed class Load<out T> {
        data object Idle : Load<Nothing>()
        data object Loading : Load<Nothing>()
        data class Done<T>(val value: T) : Load<T>()
        data class Failed(val message: String) : Load<Nothing>()
    }

    private val prefs = application.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)
    private val dao = AppDatabase.getInstance(application).savedOpportunityDao()

    val profile = MutableStateFlow(IncomeProfile.load(prefs))
    val saved: StateFlow<List<SavedOpportunity>> = dao.all().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val jobQuery = MutableStateFlow(profile.value.jobQuery)
    val remoteOnly = MutableStateFlow(true)
    /** Show only listings that are digital tasks Kora can draft. */
    val draftableOnly = MutableStateFlow(false)
    val jobs = MutableStateFlow<Load<JobBoards.Result>>(Load.Idle)
    val incomeIdeas = MutableStateFlow<Load<IdeaEngine.Batch>>(Load.Idle)
    val buildTopic = MutableStateFlow("")
    val buildIdeas = MutableStateFlow<Load<IdeaEngine.Batch>>(Load.Idle)

    /** One-line messages for a snackbar. */
    val notice = MutableStateFlow<String?>(null)

    private var jobsJob: Job? = null

    // Earnings tracker
    private val earningDao = AppDatabase.getInstance(application).earningDao()
    val earnings: StateFlow<List<Earning>> = earningDao.all().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val currency = MutableStateFlow(EarningsStore.currency(application))
    val goalCents = MutableStateFlow(EarningsStore.goalCents(application))

    fun addEarning(source: String, amountText: String, hoursText: String, note: String, earnedAt: Long): Boolean {
        val cents = EarningsMath.parseAmount(amountText) ?: return false
        val minutes = hoursText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= 1000 }?.let { (it * 60).toInt() }
        viewModelScope.launch {
            earningDao.insert(Earning(source = source.trim().ifEmpty { "Other" }.take(60), amountCents = cents, minutes = minutes, note = note.trim().take(200), earnedAt = earnedAt))
            notice.value = "Logged ${EarningsMath.format(cents, currency.value)}"
        }
        return true
    }

    fun deleteEarning(id: Int) {
        viewModelScope.launch { earningDao.delete(id) }
    }

    fun saveEarningsSettings(code: String, goalText: String) {
        val clean = code.trim().uppercase().take(3).takeIf { c -> runCatching { java.util.Currency.getInstance(c) }.isSuccess } ?: currency.value
        val goal = if (goalText.isBlank()) 0L else EarningsMath.parseAmount(goalText) ?: goalCents.value
        EarningsStore.saveSettings(getApplication(), clean, goal)
        currency.value = clean
        goalCents.value = goal
    }

    /** A Sunday-evening task where Kora reviews the tracker and suggests what to focus on. */
    fun createMoneyCheckIn() {
        viewModelScope.launch {
            TaskScheduler.save(
                getApplication(),
                ScheduledTask(
                    title = "Weekly money check-in",
                    prompt = "Call read_earnings. Review my week: what I earned vs my monthly goal, my real hourly rate per income source, " +
                        "which source is worth more of my time and which to drop or reprice. Give me 3 concrete actions for next week. Be honest and brief.",
                    hour = 18,
                    minute = 0,
                    repeat = ScheduledTask.REPEAT_SUNDAYS,
                    useWeb = false
                )
            )
            notice.value = "Kora will review your earnings with you"
        }
    }

    fun saveProfile(updated: IncomeProfile) {
        profile.value = updated
        updated.save(prefs)
        if (jobQuery.value.isBlank()) jobQuery.value = updated.jobQuery
    }

    /** Live web is the user's privacy switch for everything that leaves the phone to search. */
    private fun webAllowed(): Boolean = prefs.getBoolean("live_internet_enabled", true)

    fun searchJobs() {
        if (!webAllowed()) {
            jobs.value = Load.Failed("Live web is off. Turn it on in the chat's Tools to search job boards.")
            return
        }
        val query = jobQuery.value.trim()
        jobsJob?.cancel()
        jobsJob = viewModelScope.launch {
            jobs.value = Load.Loading
            val result = JobBoards.search(query, remoteOnly.value)
            jobs.value = if (result.jobs.isEmpty() && result.failedSources.size == 3) {
                Load.Failed("The job boards couldn't be reached. Check your connection and try again.")
            } else {
                Load.Done(result)
            }
        }
    }

    fun findIncomeIdeas() = generate(incomeIdeas) { settings, models -> IdeaEngine.sideIncome(settings, models, profile.value) }

    fun findBuildIdeas() = generate(buildIdeas) { settings, models -> IdeaEngine.builds(settings, models, profile.value, buildTopic.value.trim()) }

    private fun generate(target: MutableStateFlow<Load<IdeaEngine.Batch>>, block: suspend (LlmSettings, List<String>) -> IdeaEngine.Batch) {
        if (target.value is Load.Loading) return
        if (!webAllowed()) {
            target.value = Load.Failed("Live web is off. Ideas are checked against live sources, so turn it on in the chat's Tools first.")
            return
        }
        val settings = LlmSettings.fromPrefs(prefs)
        if (!settings.isReady) {
            target.value = Load.Failed("Add your Gemini API key or server in Settings first.")
            return
        }
        viewModelScope.launch {
            target.value = Load.Loading
            target.value = try {
                Load.Done(block(settings, AgentRunner.modelsFor(settings, prefs.getString("selected_model", null).orEmpty())))
            } catch (e: CancellationException) {
                throw e
            } catch (e: retrofit2.HttpException) {
                Load.Failed(if (e.code() == 429) "Your AI quota is used up for now. Try again later." else "The AI request failed (${e.code()}).")
            } catch (e: Exception) {
                Load.Failed(e.message ?: "Something went wrong. Try again.")
            }
        }
    }

    fun saveJob(job: JobListing) = save(
        SavedOpportunity(
            kind = SavedOpportunity.KIND_JOB,
            title = job.title,
            subtitle = listOf(job.company, job.source).filter { it.isNotEmpty() }.joinToString(" · "),
            url = job.url,
            details = listOf(job.location, job.jobType, job.salary, job.excerpt).filter { it.isNotEmpty() }.joinToString("\n\n")
        )
    )

    fun saveIdea(idea: Idea, kind: String) = save(
        SavedOpportunity(kind = kind, title = idea.title, subtitle = idea.summary.take(140), url = idea.sources.firstOrNull(), details = idea.toMarkdown())
    )

    private fun save(item: SavedOpportunity) {
        viewModelScope.launch {
            if (dao.countSame(item.kind, item.title, item.url) > 0) {
                notice.value = "Already saved"
            } else {
                dao.insert(item)
                notice.value = "Saved to your list"
            }
        }
    }

    fun setStatus(id: Int, status: String) {
        viewModelScope.launch { dao.setStatus(id, status, System.currentTimeMillis()) }
    }

    fun delete(id: Int) {
        viewModelScope.launch { dao.delete(id) }
    }

    /** A weekday-morning task that searches the boards for the user's skills and notifies them. */
    fun createJobScout() {
        val query = jobQuery.value.ifBlank { profile.value.jobQuery }.ifBlank { return run { notice.value = "Add your skills or a search first" } }
        viewModelScope.launch {
            TaskScheduler.save(
                getApplication(),
                ScheduledTask(
                    title = "Job scout: $query",
                    prompt = "Call search_jobs for \"$query\"" + (if (remoteOnly.value) " (remote only)" else "") +
                        ". Pick the 5 listings that best fit my profile, newest first. For each: title, company, link, and one line on why it fits. " +
                        "Skip anything that looks like a scam (pay to apply, vague company, crypto or task-app schemes) and say so.",
                    hour = 8,
                    minute = 0,
                    repeat = ScheduledTask.REPEAT_WEEKDAYS,
                    useWeb = true
                )
            )
            notice.value = "Kora will scout new \"$query\" jobs every weekday at 8:00"
        }
    }
}
