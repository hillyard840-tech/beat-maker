package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Earning
import com.example.data.SavedOpportunity
import com.example.income.Idea
import com.example.income.AgentBoards
import com.example.income.Deliverables
import com.example.income.EarningsMath
import com.example.income.IdeaEngine
import com.example.income.IncomeProfile
import com.example.income.JobBoards
import com.example.income.JobListing
import com.example.income.OpportunitiesViewModel
import com.example.income.OpportunitiesViewModel.Load
import com.example.util.AgentTime

private enum class OppTab(val label: String) { JOBS("Jobs"), INCOME("Side income"), BUILD("Build"), EARNINGS("Earnings"), SAVED("My list") }

/** What the Opportunities screen can hand back to the rest of the app. */
class OpportunityActions(
    val askKora: (String) -> Unit,
    val practiceInterview: (title: String, company: String, details: String) -> Unit,
    val scamCheck: (String) -> Unit,
    val openCanvas: (String, String) -> Unit,
    val openAgentJobs: () -> Unit
)

/**
 * Kora's income hub: live remote jobs, side-income ideas and program ideas backed by real
 * demand, plus a list to track what the user is pursuing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesScreen(
    onBack: () -> Unit,
    actions: OpportunityActions,
    vm: OpportunitiesViewModel = viewModel()
) {
    val onAskKora = actions.askKora
    val onOpenCanvas = actions.openCanvas
    val profile by vm.profile.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val jobsState by vm.jobs.collectAsStateWithLifecycle()
    val query by vm.jobQuery.collectAsStateWithLifecycle()
    val remote by vm.remoteOnly.collectAsStateWithLifecycle()
    val incomeState by vm.incomeIdeas.collectAsStateWithLifecycle()
    val buildState by vm.buildIdeas.collectAsStateWithLifecycle()
    val topic by vm.buildTopic.collectAsStateWithLifecycle()
    val draftableOnly by vm.draftableOnly.collectAsStateWithLifecycle()
    val earnings by vm.earnings.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    val goalCents by vm.goalCents.collectAsStateWithLifecycle()
    var showScamDialog by remember { mutableStateOf(false) }
    var showAddEarning by remember { mutableStateOf(false) }
    var showEarningsSettings by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(OppTab.JOBS) }
    var editingProfile by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        notice?.let {
            snackbar.showSnackbar(it)
            vm.notice.value = null
        }
    }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opportunities") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { editingProfile = true }) { Icon(Icons.Default.ManageAccounts, contentDescription = "Edit my profile") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp) {
                OppTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = { Text(if (t == OppTab.SAVED && saved.isNotEmpty()) "${t.label} (${saved.size})" else t.label) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (profile.isEmpty && tab != OppTab.SAVED && tab != OppTab.EARNINGS) {
                    item { ProfilePrompt { editingProfile = true } }
                }
                when (tab) {
                    OppTab.JOBS -> jobsTab(jobsState, query, remote, draftableOnly, vm, actions, profile) { showScamDialog = true }
                    OppTab.EARNINGS -> earningsTab(
                        earnings, currency, goalCents, vm,
                        onAdd = { showAddEarning = true },
                        onSettings = { showEarningsSettings = true },
                        onAskKora = onAskKora
                    )
                    OppTab.INCOME -> ideasTab(
                        state = incomeState,
                        intro = "Ways to earn that fit your skills, time and budget, checked against live web results.",
                        button = "Find ideas for me",
                        kind = SavedOpportunity.KIND_INCOME,
                        onGenerate = vm::findIncomeIdeas,
                        vm = vm,
                        onAskKora = onAskKora,
                        onOpenCanvas = onOpenCanvas
                    )
                    OppTab.BUILD -> {
                        item {
                            OutlinedTextField(
                                value = topic,
                                onValueChange = { vm.buildTopic.value = it.take(80) },
                                label = { Text("Area (optional)") },
                                placeholder = { Text(profile.interests.ifBlank { "e.g. tools for small shops" }) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ideasTab(
                            state = buildState,
                            intro = "Programs worth building, based on real posts where people describe a problem or ask for a tool.",
                            button = "Find real demand",
                            kind = SavedOpportunity.KIND_BUILD,
                            onGenerate = vm::findBuildIdeas,
                            vm = vm,
                            onAskKora = onAskKora,
                            onOpenCanvas = onOpenCanvas
                        )
                    }
                    OppTab.SAVED -> savedTab(saved, vm, actions)
                }
            }
        }
    }

    if (showScamDialog) {
        ScamCheckDialog(onDismiss = { showScamDialog = false }) {
            showScamDialog = false
            actions.scamCheck(it)
        }
    }
    if (showAddEarning) {
        AddEarningDialog(
            currency = currency,
            suggestions = earnings.map { it.source }.distinct().take(6),
            onDismiss = { showAddEarning = false },
            onAdd = { source, amount, hours, note, at -> vm.addEarning(source, amount, hours, note, at).also { if (it) showAddEarning = false } }
        )
    }
    if (showEarningsSettings) {
        EarningsSettingsDialog(currency, goalCents, onDismiss = { showEarningsSettings = false }) { code, goal ->
            vm.saveEarningsSettings(code, goal)
            showEarningsSettings = false
        }
    }

    if (editingProfile) {
        ProfileDialog(profile, onDismiss = { editingProfile = false }) {
            vm.saveProfile(it)
            editingProfile = false
        }
    }
}

@Composable
private fun ProfilePrompt(onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Tell Kora your skills and goals for better matches. It stays on your phone.", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onEdit) { Text("Set up") }
        }
    }
}

// ---------------------------------------------------------------- jobs

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.jobsTab(
    state: Load<JobBoards.Result>,
    query: String,
    remote: Boolean,
    draftableOnly: Boolean,
    vm: OpportunitiesViewModel,
    actions: OpportunityActions,
    profile: IncomeProfile,
    onScamCheck: () -> Unit
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.jobQuery.value = it.take(60) },
                label = { Text("Skill or role") },
                placeholder = { Text("e.g. python, copywriting, customer support") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.searchJobs() }),
                trailingIcon = { IconButton(onClick = { vm.searchJobs() }) { Icon(Icons.Default.Search, contentDescription = "Search jobs") } },
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = remote, onClick = { vm.remoteOnly.value = !remote }, label = { Text("Remote only") })
                FilterChip(
                    selected = draftableOnly,
                    onClick = { vm.draftableOnly.value = !draftableOnly },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Kora can do it") }
                )
                AssistChip(
                    onClick = { vm.createJobScout() },
                    leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Scout every weekday") }
                )
            }
        }
    }
    item { ScamCheckCard(onScamCheck) }
    item {
        when (val s = state) {
            Load.Idle -> Text(
                "Searches Remotive, Jobicy and Arbeitnow live. Only real listings are shown, each with a link to apply.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Load.Loading -> LoadingRow("Searching job boards…")
            is Load.Failed -> ErrorRow(s.message) { vm.searchJobs() }
            is Load.Done -> JobsHeader(s.value)
        }
    }
    if (state is Load.Done) {
        val shown = if (draftableOnly) state.value.jobs.filter { draftKind(it) != null } else state.value.jobs
        if (draftableOnly && shown.isEmpty()) {
            item {
                Text(
                    "None of these listings are tasks Kora can draft. Most are ongoing jobs; try searching \"writing\", \"research\" or \"data entry\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(shown, key = { it.url }) { job -> JobCard(job, onSave = { vm.saveJob(job) }, actions = actions, profile = profile) }
    }
    agentBoardsSection(actions)
}

@Composable
private fun JobsHeader(result: JobBoards.Result) {
    Column {
        Text(
            if (result.jobs.isEmpty()) "No listings match right now. Try a broader skill." else "${result.jobs.size} live listings",
            style = MaterialTheme.typography.titleSmall
        )
        if (result.failedSources.isNotEmpty()) {
            Text(
                "Couldn't reach: ${result.failedSources.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobCard(job: JobListing, onSave: () -> Unit, actions: OpportunityActions, profile: IncomeProfile) {
    val uri = LocalUriHandler.current
    val onAskKora = actions.askKora
    val kind = remember(job.url) { draftKind(job) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(job.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(job.company, job.location).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val meta = listOfNotNull(
                job.jobType.ifEmpty { null },
                job.salary.ifEmpty { null },
                job.postedAt?.let { "posted ${AgentTime.label(it).substringBeforeLast(',')}" },
                "via ${job.source}"
            )
            Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            if (job.excerpt.isNotEmpty()) {
                Text(job.excerpt, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, end = 8.dp))
            }
            if (kind != null) KoraCanDoBadge(kind)
            FlowRow {
                TextButton(onClick = { uri.safeOpen(job.url) }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open")
                }
                TextButton(onClick = onSave) { Text("Save") }
                TextButton(onClick = {
                    onAskKora(
                        "Help me apply for this job: \"${job.title}\" at ${job.company.ifEmpty { "an unlisted company" }} (${job.url}).\n" +
                            (if (job.excerpt.isNotEmpty()) "From the listing: ${job.excerpt}\n" else "") +
                            "Read the listing, tell me honestly how well I fit" + (if (profile.skills.isNotBlank()) " with my skills (${profile.skills})" else "") +
                            ", flag anything that looks like a scam, and draft a short, specific cover note."
                    )
                }) { Text("Apply with Kora") }
                if (kind != null) {
                    TextButton(onClick = {
                        onAskKora(Deliverables.draftPrompt(job.title, job.excerpt, job.source, job.url, kind))
                    }) { Text("Draft it with Kora") }
                }
                TextButton(onClick = {
                    actions.practiceInterview(job.title, job.company, listOf(job.jobType, job.location, job.excerpt).filter { it.isNotEmpty() }.joinToString("\n"))
                }) { Text("Practice interview") }
            }
        }
    }
}

@Composable
private fun AgentJobsCard(onOpen: () -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SmartToy, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Live Claw Earn tasks", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Browse open tasks inside Kora and tap one to have Kora draft the work for you to check.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

// ---------------------------------------------------------------- ideas

private fun LazyListScope.ideasTab(
    state: Load<IdeaEngine.Batch>,
    intro: String,
    button: String,
    kind: String,
    onGenerate: () -> Unit,
    vm: OpportunitiesViewModel,
    onAskKora: (String) -> Unit,
    onOpenCanvas: (String, String) -> Unit
) {
    item {
        val s = state
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(intro, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onGenerate, enabled = s !is Load.Loading) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (s is Load.Done) "Try again" else button)
            }
            when (val v = s) {
                Load.Loading -> LoadingRow("Checking live sources and thinking…")
                is Load.Failed -> ErrorRow(v.message, onGenerate)
                is Load.Done -> {
                    v.value.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    Text(
                        "AI suggestions with rough estimates, not promises. Check the sources before you spend money.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Load.Idle -> {}
            }
        }
    }
    val current = state
    if (current is Load.Done) {
        items(current.value.ideas, key = { it.title }) { idea ->
            IdeaCard(
                idea = idea,
                isBuild = kind == SavedOpportunity.KIND_BUILD,
                onSave = { vm.saveIdea(idea, kind) },
                onPlan = {
                    onAskKora(
                        if (kind == SavedOpportunity.KIND_BUILD) "Let's build \"${idea.title}\". ${idea.summary}\nGive me a concrete MVP plan: screens or endpoints, data model, the first week of tasks, and how to get the first 3 paying users."
                        else "Help me start \"${idea.title}\". ${idea.summary}\nMake a realistic 30-day plan with weekly goals, what to set up first, and how to land the first paying customer."
                    )
                },
                onCanvas = { onOpenCanvas(idea.title, idea.toMarkdown()) }
            )
        }
        if (current.value.evidence.isNotEmpty()) {
            item { EvidenceList(current.value.evidence) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdeaCard(idea: Idea, isBuild: Boolean, onSave: () -> Unit, onPlan: () -> Unit, onCanvas: () -> Unit) {
    var expanded by rememberSaveable(idea.title) { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    ElevatedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(idea.title, style = MaterialTheme.typography.titleMedium)
            Text(idea.summary, style = MaterialTheme.typography.bodyMedium, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOfNotNull(
                    idea.startupCost.ifEmpty { null }?.let { "💵 $it" },
                    idea.timeToFirstIncome.ifEmpty { null }?.let { "⏱ $it" },
                    idea.whoPays.ifEmpty { null }?.let { "👥 $it" }
                ).forEach { SuggestionChip(onClick = {}, label = { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }) }
            }
            if (expanded) {
                if (idea.earnings.isNotEmpty()) Text("Realistic earnings: ${idea.earnings}", style = MaterialTheme.typography.bodySmall)
                if (idea.stack.isNotEmpty()) Text("Stack: ${idea.stack}", style = MaterialTheme.typography.bodySmall)
                if (idea.steps.isNotEmpty()) {
                    Text(if (isBuild) "MVP features" else "First steps", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                    idea.steps.forEachIndexed { i, step -> Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall) }
                }
                if (idea.risks.isNotEmpty()) Text("Watch out: ${idea.risks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                idea.sources.forEach { url ->
                    Text(
                        url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 8.dp).clickable { uri.safeOpen(url) }
                    )
                }
            }
            FlowRow {
                TextButton(onClick = onPlan) { Text(if (isBuild) "Plan the build" else "Plan it with Kora") }
                TextButton(onClick = onSave) { Text("Save") }
                if (isBuild) TextButton(onClick = onCanvas) { Text("Open in Canvas") }
                if (!expanded && idea.sources.isNotEmpty()) TextButton(onClick = { uri.safeOpen(idea.sources.first()) }) { Text("Source") }
            }
        }
    }
}

@Composable
private fun EvidenceList(evidence: List<Pair<String, String>>) {
    val uri = LocalUriHandler.current
    Column(Modifier.padding(top = 8.dp)) {
        Text("Live sources Kora looked at", style = MaterialTheme.typography.titleSmall)
        evidence.take(10).forEach { (title, url) ->
            TextButton(onClick = { uri.safeOpen(url) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------------------------------------------------------------- saved

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.savedTab(saved: List<SavedOpportunity>, vm: OpportunitiesViewModel, actions: OpportunityActions) {
    val onAskKora = actions.askKora
    if (saved.isEmpty()) {
        item {
            Text(
                "Nothing saved yet. Save jobs and ideas to track them here: saved, applied or started, then done.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    items(saved, key = { it.id }) { item ->
        val uri = LocalUriHandler.current
        var confirmDelete by remember { mutableStateOf(false) }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (item.kind) {
                            SavedOpportunity.KIND_JOB -> Icons.Default.Work
                            SavedOpportunity.KIND_BUILD -> Icons.Default.Construction
                            else -> Icons.Default.Savings
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Close, contentDescription = "Remove") }
                }
                if (item.subtitle.isNotEmpty()) Text(item.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    val active = if (item.kind == SavedOpportunity.KIND_JOB) "Applied" else "Started"
                    val done = if (item.kind == SavedOpportunity.KIND_JOB) "Got it" else "Earning"
                    listOf(
                        SavedOpportunity.STATUS_SAVED to "Saved",
                        SavedOpportunity.STATUS_ACTIVE to active,
                        SavedOpportunity.STATUS_DONE to done,
                        SavedOpportunity.STATUS_DROPPED to "Dropped"
                    ).forEach { (status, label) ->
                        FilterChip(selected = item.status == status, onClick = { vm.setStatus(item.id, status) }, label = { Text(label) })
                    }
                }
                FlowRow {
                    item.url?.let { url -> TextButton(onClick = { uri.safeOpen(url) }) { Text("Open") } }
                    TextButton(onClick = {
                        onAskKora(
                            "I'm working on this (${item.status}): ${item.title}\n\n${item.details.take(1500)}\n\nWhat should my next step be this week?"
                        )
                    }) { Text("Next step with Kora") }
                    if (item.kind == SavedOpportunity.KIND_JOB) {
                        TextButton(onClick = {
                            actions.practiceInterview(item.title, item.subtitle.substringBefore(" · "), item.details)
                        }) { Text("Practice interview") }
                    }
                }
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Remove from your list?") },
                confirmButton = { TextButton(onClick = { vm.delete(item.id); confirmDelete = false }) { Text("Remove") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
            )
        }
    }
}

// ---------------------------------------------------------------- shared

@Composable
private fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileDialog(initial: IncomeProfile, onDismiss: () -> Unit, onSave: (IncomeProfile) -> Unit) {
    var skills by remember { mutableStateOf(initial.skills) }
    var interests by remember { mutableStateOf(initial.interests) }
    var hours by remember { mutableStateOf(initial.hoursPerWeek.toFloat()) }
    var budget by remember { mutableStateOf(initial.budget) }
    var goal by remember { mutableStateOf(initial.goal) }
    var location by remember { mutableStateOf(initial.location) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My work profile") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kora uses this to match jobs and ideas. It's stored on this phone and sent to the AI only with your requests.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(skills, { skills = it.take(200) }, label = { Text("Skills") }, placeholder = { Text("e.g. Kotlin, video editing, bookkeeping") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(interests, { interests = it.take(200) }, label = { Text("Interests") }, placeholder = { Text("e.g. fitness, music production, AI") }, modifier = Modifier.fillMaxWidth())
                Text("About ${hours.toInt()} hours a week", style = MaterialTheme.typography.labelLarge)
                Slider(value = hours, onValueChange = { hours = it }, valueRange = 1f..40f, steps = 38)
                OutlinedTextField(budget, { budget = it.take(60) }, label = { Text("Money you can put in") }, placeholder = { Text("e.g. \$0, under \$100") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it.take(60) }, label = { Text("Country or city (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Main goal", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(IncomeProfile.GOAL_SIDE to "Side income", IncomeProfile.GOAL_JOB to "New job", IncomeProfile.GOAL_BUSINESS to "Build a product").forEach { (id, label) ->
                        FilterChip(selected = goal == id, onClick = { goal = id }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(IncomeProfile(skills, interests, hours.toInt(), budget, goal, location)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Opens a link in the browser; does nothing if no browser is installed. */
private fun androidx.compose.ui.platform.UriHandler.safeOpen(url: String) {
    try {
        openUri(url)
    } catch (_: Exception) {
    }
}


private fun draftKind(job: JobListing): Deliverables.Kind? =
    Deliverables.kindOf(job.title, job.excerpt, job.tags + listOf(job.jobType).filter { it.isNotEmpty() })

@Composable
private fun KoraCanDoBadge(kind: Deliverables.Kind) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Kora can draft this (${kind.label}) for you to review", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ---------------------------------------------------------------- scam check

@Composable
private fun ScamCheckCard(onOpen: () -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Got an offer? Check it for scams", style = MaterialTheme.typography.titleSmall)
                Text("Paste it here, or use Share → Kora from any app.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ScamCheckDialog(onDismiss: () -> Unit, onCheck: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scam check") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(8000) },
                label = { Text("Paste the message, job post or link") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onCheck(text) }) { Text("Check it") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------- earnings

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.earningsTab(
    earnings: List<Earning>,
    currency: String,
    goalCents: Long,
    vm: OpportunitiesViewModel,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onAskKora: (String) -> Unit
) {
    val summary = EarningsMath.summarize(earnings.map { EarningsMath.Entry(it.source, it.amountCents, it.minutes, it.earnedAt) }, goalCents, System.currentTimeMillis())
    item {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("This month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(EarningsMath.format(summary.monthCents, currency), style = MaterialTheme.typography.headlineMedium)
                summary.goalProgress?.let { p ->
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "${(p * 100).toInt()}% of your ${EarningsMath.format(summary.goalCents, currency)} goal",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Last month ${EarningsMath.format(summary.lastMonthCents, currency)} · all time ${EarningsMath.format(summary.allTimeCents, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log earnings")
                    }
                    OutlinedButton(onClick = onSettings) { Text(if (goalCents > 0) "Goal & currency" else "Set a goal") }
                }
            }
        }
    }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { vm.createMoneyCheckIn() },
                leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text("Weekly check-in (Sundays)") }
            )
            if (earnings.isNotEmpty()) {
                AssistChip(
                    onClick = { onAskKora("Call read_earnings and review my side income honestly: which source pays best per hour, what to drop or reprice, and 3 actions for this week.") },
                    leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Review with Kora") }
                )
            }
        }
    }
    if (earnings.isEmpty()) {
        item {
            Text(
                "Log what each gig or job actually pays (and how long it took) to see your real hourly rate per hustle. You can also tell Kora in chat: \"I made \$40 on Fiverr today in 2 hours\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    item { Text("By source", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp)) }
    items(summary.bySource, key = { "src_" + it.source.lowercase() }) { s ->
        ListItem(
            headlineContent = { Text(s.source) },
            supportingContent = {
                Text(
                    "${s.entries} entr${if (s.entries == 1) "y" else "ies"} · " +
                        (s.hourlyCents?.let { "${EarningsMath.format(it, currency)}/hour" } ?: "log hours to see your hourly rate")
                )
            },
            trailingContent = { Text(EarningsMath.format(s.totalCents, currency), style = MaterialTheme.typography.titleSmall) }
        )
    }
    item { Text("Recent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp)) }
    items(earnings.take(30), key = { "e" + it.id }) { e ->
        ListItem(
            headlineContent = { Text("${EarningsMath.format(e.amountCents, currency)} · ${e.source}") },
            supportingContent = {
                Text(
                    listOfNotNull(
                        AgentTime.label(e.earnedAt).substringBeforeLast(','),
                        e.minutes?.let { "${it / 60}h ${it % 60}m" },
                        e.note.ifEmpty { null }
                    ).joinToString(" · ")
                )
            },
            trailingContent = { IconButton(onClick = { vm.deleteEarning(e.id) }) { Icon(Icons.Default.Close, contentDescription = "Delete entry") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEarningDialog(
    currency: String,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onAdd: (source: String, amount: String, hours: String, note: String, earnedAt: Long) -> Boolean
) {
    var source by remember { mutableStateOf(suggestions.firstOrNull().orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var daysAgo by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log earnings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(source, { source = it.take(60) }, label = { Text("Source") }, placeholder = { Text("e.g. Fiverr logo gigs") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (suggestions.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { s -> SuggestionChip(onClick = { source = s }, label = { Text(s) }) }
                    }
                }
                OutlinedTextField(
                    amount, { amount = it.take(12); error = false },
                    label = { Text("Amount ($currency)") },
                    isError = error,
                    supportingText = if (error) { { Text("Enter an amount like 45.50") } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    hours, { hours = it.take(6) },
                    label = { Text("Hours spent (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Today", 1 to "Yesterday", 7 to "Last week").forEach { (d, label) ->
                        FilterChip(selected = daysAgo == d, onClick = { daysAgo = d }, label = { Text(label) })
                    }
                }
                OutlinedTextField(note, { note = it.take(200) }, label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = source.isNotBlank() && amount.isNotBlank(), onClick = {
                val at = System.currentTimeMillis() - daysAgo * 24L * 60 * 60 * 1000
                error = !onAdd(source, amount, hours, note, at)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EarningsSettingsDialog(currency: String, goalCents: Long, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var code by remember { mutableStateOf(currency) }
    var goal by remember { mutableStateOf(if (goalCents > 0) "%.2f".format(java.util.Locale.US, goalCents / 100.0) else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Goal & currency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(goal, { goal = it.take(12) }, label = { Text("Monthly goal (leave empty for none)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(code, { code = it.take(3).uppercase() }, label = { Text("Currency code, e.g. USD, EUR, GBP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(code, goal) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


// ---------------------------------------------------------------- jobs for AI agents

private fun LazyListScope.agentBoardsSection(actions: OpportunityActions) {
    item {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Jobs for AI agents", style = MaterialTheme.typography.titleMedium)
            Text(
                "Boards where AI agents take paid tasks. Kora can draft the work; claiming, staking and getting paid happen with your own wallet on each site.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AgentBoardWarning()
        }
    }
    item { AgentJobsCard(actions.openAgentJobs) }
    item {
        AgentBoardHeader(
            title = "Escrow confirmed (${AgentBoards.escrowConfirmed.size})",
            text = "Escrow is described in the board's own docs or open-source code, or reported by an independent outlet. Not audited on-chain, so still check before staking."
        )
    }
    items(AgentBoards.escrowConfirmed, key = { "board_" + it.name }) { AgentBoardCard(it, actions) }
    item {
        AgentBoardHeader(
            title = "Escrow not verified (${AgentBoards.notVerified.size})",
            text = "Escrow is only claimed by listings, unclear, missing, or the board has reported payout problems. Treat as high risk."
        )
    }
    items(AgentBoards.notVerified, key = { "board_" + it.name }) { AgentBoardCard(it, actions) }
    item {
        Text(
            "Checked ${AgentBoards.CHECKED} from public sources. These boards change fast.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AgentBoardWarning() {
    val uri = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Before you connect a wallet", style = MaterialTheme.typography.titleSmall)
            }
            Text(AgentBoards.WARNING, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            TextButton(onClick = { uri.safeOpen(AgentBoards.WARNING_SOURCE) }, contentPadding = PaddingValues(0.dp)) { Text("Source") }
        }
    }
}

@Composable
private fun AgentBoardHeader(title: String, text: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentBoardCard(board: AgentBoards.Board, actions: OpportunityActions) {
    val uri = LocalUriHandler.current
    val confirmed = board.tier == AgentBoards.Tier.ESCROW_CONFIRMED
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (confirmed) Icons.Default.VerifiedUser else Icons.Default.GppMaybe,
                    contentDescription = if (confirmed) "Escrow confirmed" else "Escrow not verified",
                    tint = if (confirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(board.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(board.chain, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
            }
            Text(board.summary, style = MaterialTheme.typography.bodySmall)
            Text("Escrow: ${board.escrow}", style = MaterialTheme.typography.bodySmall)
            Text(board.caveat, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow {
                if (board.liveInApp) TextButton(onClick = actions.openAgentJobs) { Text("Live tasks in Kora") }
                board.url?.let { url -> TextButton(onClick = { uri.safeOpen(url) }) { Text("Open site") } }
                TextButton(onClick = { uri.safeOpen(board.sourceUrl) }) { Text(if (board.url == null) "Read about it" else "Source") }
                TextButton(onClick = {
                    actions.askKora(
                        "I'm looking at ${board.name} (${board.url ?: board.sourceUrl}), a job board for AI agents. What I know: ${board.summary} ${board.escrow} ${board.caveat}\n" +
                            "Check it for me with the web: is it active, does it really lock money in escrow before work starts, are there reports of real payouts or problems, and what would I risk by connecting a wallet or staking?"
                    )
                }) { Text("Check it with Kora") }
            }
        }
    }
}
