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
import com.example.data.SavedOpportunity
import com.example.income.Idea
import com.example.income.IdeaEngine
import com.example.income.IncomeProfile
import com.example.income.JobBoards
import com.example.income.JobListing
import com.example.income.OpportunitiesViewModel
import com.example.income.OpportunitiesViewModel.Load
import com.example.util.AgentTime

private enum class OppTab(val label: String) { JOBS("Jobs"), INCOME("Side income"), BUILD("Build"), SAVED("My list") }

/**
 * Kora's income hub: live remote jobs, side-income ideas and program ideas backed by real
 * demand, plus a list to track what the user is pursuing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesScreen(
    onBack: () -> Unit,
    onAskKora: (String) -> Unit,
    onOpenCanvas: (String, String) -> Unit,
    onOpenAgentJobs: () -> Unit,
    vm: OpportunitiesViewModel = viewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val jobsState by vm.jobs.collectAsStateWithLifecycle()
    val query by vm.jobQuery.collectAsStateWithLifecycle()
    val remote by vm.remoteOnly.collectAsStateWithLifecycle()
    val incomeState by vm.incomeIdeas.collectAsStateWithLifecycle()
    val buildState by vm.buildIdeas.collectAsStateWithLifecycle()
    val topic by vm.buildTopic.collectAsStateWithLifecycle()
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
                if (profile.isEmpty && tab != OppTab.SAVED) {
                    item { ProfilePrompt { editingProfile = true } }
                }
                when (tab) {
                    OppTab.JOBS -> jobsTab(jobsState, query, remote, vm, onAskKora, profile, onOpenAgentJobs)
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
                    OppTab.SAVED -> savedTab(saved, vm, onAskKora)
                }
            }
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
    vm: OpportunitiesViewModel,
    onAskKora: (String) -> Unit,
    profile: IncomeProfile,
    onOpenAgentJobs: () -> Unit
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
                AssistChip(
                    onClick = { vm.createJobScout() },
                    leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Scout every weekday") }
                )
            }
        }
    }
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
        items(state.value.jobs, key = { it.url }) { job -> JobCard(job, onSave = { vm.saveJob(job) }, onAskKora = onAskKora, profile = profile) }
    }
    item { AgentJobsCard(onOpenAgentJobs) }
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
private fun JobCard(job: JobListing, onSave: () -> Unit, onAskKora: (String) -> Unit, profile: IncomeProfile) {
    val uri = LocalUriHandler.current
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
                Text("Jobs for AI agents", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Live board of paid tasks posted for AI agents (Claw Earn). Kora shows them but won't take paid work on her own: claiming one needs your wallet and your OK.",
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
private fun LazyListScope.savedTab(saved: List<SavedOpportunity>, vm: OpportunitiesViewModel, onAskKora: (String) -> Unit) {
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
