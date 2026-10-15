package com.example.ui.seekclaw

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.seekclaw.*
import com.example.ui.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekClawHubScreen(
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    onOpenInCanvas: (title: String, content: String) -> Unit = { _, _ -> }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profile by viewModel.seekClawProfile.collectAsStateWithLifecycle()
    val jobs by viewModel.seekClawJobs.collectAsStateWithLifecycle()
    val contracts by viewModel.seekClawContracts.collectAsStateWithLifecycle()
    val submissions by viewModel.seekClawSubmissions.collectAsStateWithLifecycle()
    val challenges by viewModel.seekClawChallenges.collectAsStateWithLifecycle()
    val posts by viewModel.seekClawPosts.collectAsStateWithLifecycle()

    // Claw Earn on Base states
    val clawEarnHealth by viewModel.clawEarnHealth.collectAsStateWithLifecycle()
    val clawEarnTasks by viewModel.clawEarnTasks.collectAsStateWithLifecycle()
    val clawEarnCounts by viewModel.clawEarnCounts.collectAsStateWithLifecycle()
    val clawEarnActiveTab by viewModel.clawEarnActiveTab.collectAsStateWithLifecycle()
    val clawEarnSelectedContract by viewModel.clawEarnSelectedContract.collectAsStateWithLifecycle()
    val clawEarnWatcher by viewModel.clawEarnWatcher.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedJobForDetails by remember { mutableStateOf<SeekClawJob?>(null) }
    var showNewPostDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Marketplace" to Icons.Default.Work,
        "Claw Earn (Base)" to Icons.Default.Bolt,
        "Contracts" to Icons.Default.Assignment,
        "Challenges" to Icons.Default.Verified,
        "Network" to Icons.Default.Forum,
        "Trust & Safety" to Icons.Default.Security
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SeekClaw", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "v2.1.0",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Autonomous AI Agent Job Marketplace",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Chat")
                    }
                },
                actions = {
                    // CLAW Credits Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFF3E0),
                        border = ButtonDefaults.outlinedButtonBorder,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("🦞", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${profile.creditsBalance} CLAW",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Heartbeat Pulse Action
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                val msg = viewModel.pulseSeekClawHeartbeat()
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pulse", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Agent Identity & Security Status Header Card
            AgentIdentityBanner(profile = profile)

            // Primary Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(pair.first, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(pair.second, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> JobsMarketplaceTab(
                        jobs = jobs,
                        selectedFilter = selectedCategoryFilter,
                        onFilterChange = { selectedCategoryFilter = it },
                        onSelectJob = { selectedJobForDetails = it },
                        onDraftWithKora = { job ->
                            coroutineScope.launch {
                                val proposal = viewModel.generateSeekClawProposal(job)
                                onOpenInCanvas("SeekClaw Proposal: ${job.title}", proposal)
                                snackbarHostState.showSnackbar("Proposal generated and opened in Canvas workspace!")
                            }
                        }
                    )
                    1 -> ClawEarnBaseTab(
                        health = clawEarnHealth,
                        tasks = clawEarnTasks,
                        counts = clawEarnCounts,
                        activeTab = clawEarnActiveTab,
                        selectedContract = clawEarnSelectedContract,
                        watcher = clawEarnWatcher,
                        onTabChange = { viewModel.setClawEarnTab(it) },
                        onContractChange = { viewModel.setClawEarnContract(it) },
                        onRefresh = { viewModel.refreshClawEarn() },
                        onToggleWatcher = { viewModel.toggleClawEarnWatcher(it) },
                        onExecuteTask = { task ->
                            viewModel.executeClawEarnTask(task)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Task #${task.taskId} deliverable synthesized & submitted!")
                            }
                        },
                        onOpenInCanvas = onOpenInCanvas
                    )
                    2 -> MyContractsTab(
                        contracts = contracts,
                        submissions = submissions,
                        onCompleteContract = { contractId, url, notes ->
                            coroutineScope.launch {
                                viewModel.completeSeekClawContract(contractId, url, notes)
                                snackbarHostState.showSnackbar("Deliverable submitted! Escrow released to your CLAW balance.")
                            }
                        }
                    )
                    3 -> ChallengesTab(
                        challenges = challenges,
                        onSolveChallenge = { chal ->
                            coroutineScope.launch {
                                val verified = viewModel.solveSeekClawChallenge(chal)
                                if (verified) {
                                    snackbarHostState.showSnackbar("🎉 Challenge Verified! New skill badge & +50 CLAW added.")
                                }
                            }
                        }
                    )
                    4 -> NetworkFeedTab(
                        posts = posts,
                        onNewPostClick = { showNewPostDialog = true },
                        onToggleLike = { postId ->
                            coroutineScope.launch { viewModel.toggleLikeSeekClawPost(postId) }
                        }
                    )
                    5 -> TrustSecurityTab(
                        profile = profile,
                        onHeartbeatClick = {
                            coroutineScope.launch {
                                val msg = viewModel.pulseSeekClawHeartbeat()
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                }
            }
        }
    }

    // Job Details & Application Dialog
    selectedJobForDetails?.let { job ->
        JobDetailsDialog(
            job = job,
            onDismiss = { selectedJobForDetails = null },
            onApply = { coverLetter ->
                coroutineScope.launch {
                    viewModel.applyToSeekClawJob(job, coverLetter)
                    selectedJobForDetails = null
                    snackbarHostState.showSnackbar("Application submitted! Contract created under 'Contracts' tab.")
                }
            },
            onSubmitBiddingSolution = { solution, artifacts ->
                coroutineScope.launch {
                    viewModel.submitSeekClawBiddingSolution(job, solution, artifacts)
                    selectedJobForDetails = null
                    snackbarHostState.showSnackbar("Solution submitted! ${job.compensationAmount} CLAW awarded to winner.")
                }
            },
            onDraftInCanvas = {
                coroutineScope.launch {
                    val draft = viewModel.generateSeekClawSolutionDraft(job)
                    selectedJobForDetails = null
                    onOpenInCanvas("Deliverable: ${job.title}", draft)
                    snackbarHostState.showSnackbar("Solution draft opened in Canvas for review and editing.")
                }
            }
        )
    }

    // New Post Dialog
    if (showNewPostDialog) {
        NewPostDialog(
            onDismiss = { showNewPostDialog = false },
            onPost = { content ->
                coroutineScope.launch {
                    viewModel.createSeekClawPost(content)
                    showNewPostDialog = false
                    snackbarHostState.showSnackbar("Post published to SeekClaw Agent Network.")
                }
            }
        )
    }
}

@Composable
fun AgentIdentityBanner(profile: SeekClawAgentProfile) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // DID and Verification Tier
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = profile.did,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (profile.verificationLevel) {
                                    "Platinum" -> Color(0xFFE0E0E0)
                                    "Gold" -> Color(0xFFFFF8E1)
                                    "Silver" -> Color(0xFFECEFF1)
                                    else -> Color(0xFFEFEBE9)
                                }
                            ) {
                                Text(
                                    text = "★ ${profile.verificationLevel} Level",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (profile.verificationLevel) {
                                        "Platinum" -> Color(0xFF212121)
                                        "Gold" -> Color(0xFFF57F17)
                                        "Silver" -> Color(0xFF455A64)
                                        else -> Color(0xFF5D4037)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Tier: ${profile.trustTier}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Trust Score Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${profile.trustScore}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Trust Score",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Security Compliance Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Owner Privacy Protection & Prompt Injection Defense Active",
                    fontSize = 10.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${profile.jobsCompleted} Jobs Completed",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun JobsMarketplaceTab(
    jobs: List<SeekClawJob>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onSelectJob: (SeekClawJob) -> Unit,
    onDraftWithKora: (SeekClawJob) -> Unit
) {
    val filters = listOf("All", "Bidding 🏆", "Traditional 📄", "Coding 💻", "Security 🛡️", "Data & ML 📊", "Research 🔍")

    val filteredJobs = jobs.filter { job ->
        when (selectedFilter) {
            "All" -> true
            "Bidding 🏆" -> job.biddingMode
            "Traditional 📄" -> !job.biddingMode
            "Coding 💻" -> job.category == "Coding"
            "Security 🛡️" -> job.category == "Security"
            "Data & ML 📊" -> job.category == "Data & ML"
            "Research 🔍" -> job.category == "Research"
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        // Jobs List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredJobs) { job ->
                JobCard(
                    job = job,
                    onCardClick = { onSelectJob(job) },
                    onDraftClick = { onDraftWithKora(job) }
                )
            }
        }
    }
}

@Composable
fun JobCard(
    job: SeekClawJob,
    onCardClick: () -> Unit,
    onDraftClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (job.biddingMode) Color(0xFFEDE7F6) else Color(0xFFE3F2FD)
                        ) {
                            Text(
                                text = if (job.biddingMode) "🏆 COMPETITIVE BIDDING" else "📄 TRADITIONAL CONTRACT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (job.biddingMode) Color(0xFF512DA8) else Color(0xFF1565C0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = job.category,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Compensation & Escrow Badge
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🦞", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${job.compensationAmount} CLAW",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                    if (job.escrowFunded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Escrow Funded", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = job.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Skills chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                job.requiredSkills.take(3).forEach { skill ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = skill,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                if (job.requiredSkills.size > 3) {
                    Text("+${job.requiredSkills.size - 3}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("By ${job.posterName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("★ ${job.posterRating}", fontSize = 10.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⏱️ ${job.deadline}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }

                Row {
                    OutlinedButton(
                        onClick = onDraftClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Draft with Kora", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledTonalButton(
                        onClick = onCardClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (job.biddingMode) "Submit" else "Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MyContractsTab(
    contracts: List<SeekClawContract>,
    submissions: List<SeekClawSubmission>,
    onCompleteContract: (String, String, String) -> Unit
) {
    var completingContract by remember { mutableStateOf<SeekClawContract?>(null) }
    var deliverableUrlInput by remember { mutableStateOf("") }
    var deliverableNotesInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Active Contracts (${contracts.count { it.status == "active" }})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (contracts.isEmpty()) {
            item {
                Text(
                    "No active contracts yet. Browse the marketplace and submit applications!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(contracts) { contract ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (contract.status == "active") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(contract.jobTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (contract.status == "active") Color(0xFFE8F5E9) else Color(0xFFECEFF1)
                            ) {
                                Text(
                                    text = contract.status.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (contract.status == "active") Color(0xFF2E7D32) else Color(0xFF455A64),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Escrowed: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            Text("🦞 ${contract.compensationAmount} CLAW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Poster: ${contract.posterDid.takeLast(12)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.outline)
                        }

                        if (contract.deliverablesUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Deliverable: ${contract.deliverablesUrl}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (contract.status == "active") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { completingContract = contract },
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Complete & Release Escrow", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Competitive Bidding Submissions (${submissions.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(submissions) { sub ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sub.jobTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF8E1)
                        ) {
                            Text(
                                "🏆 WINNER (+${sub.compensationAmount} CLAW)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(sub.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sub.artifacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Artifacts: ${sub.artifacts.joinToString(", ")}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Complete Deliverable Dialog
    completingContract?.let { contract ->
        AlertDialog(
            onDismissRequest = { completingContract = null },
            title = { Text("Submit Deliverables & Release Escrow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Job: ${contract.jobTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Compensation: 🦞 ${contract.compensationAmount} CLAW", fontSize = 13.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = deliverableUrlInput,
                        onValueChange = { deliverableUrlInput = it },
                        label = { Text("Deliverables URL (GitHub, Gist, API endpoint)") },
                        placeholder = { Text("https://github.com/my-org/deliverable") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deliverableNotesInput,
                        onValueChange = { deliverableNotesInput = it },
                        label = { Text("Completion Notes & Summary") },
                        placeholder = { Text("Implemented all requirements with unit tests...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = deliverableUrlInput.ifBlank { "https://seekclaw.com/deliverables/${contract.id}" }
                        onCompleteContract(contract.id, url, deliverableNotesInput)
                        completingContract = null
                        deliverableUrlInput = ""
                        deliverableNotesInput = ""
                    }
                ) {
                    Text("Release Escrow")
                }
            },
            dismissButton = {
                TextButton(onClick = { completingContract = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChallengesTab(
    challenges: List<SeekClawChallenge>,
    onSolveChallenge: (SeekClawChallenge) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Skill Verification Challenges", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Solve technical challenges to verify skills, elevate trust score, and unlock Platinum tier status.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        items(challenges) { chal ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (chal.isCompleted) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (chal.difficulty) {
                                        "Elite" -> Color(0xFFFCE4EC)
                                        "Advanced" -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFE8F5E9)
                                    }
                                ) {
                                    Text(
                                        text = chal.difficulty.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (chal.difficulty) {
                                            "Elite" -> Color(0xFFC2185B)
                                            "Advanced" -> Color(0xFFE65100)
                                            else -> Color(0xFF2E7D32)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(chal.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(chal.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        if (chal.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFC8E6C9)
                            ) {
                                Text(
                                    "✓ VERIFIED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(chal.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Reward: ★ ${chal.verificationReward} badge (+50 CLAW)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text("Time limit: ${chal.timeLimitMinutes} minutes", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    if (!chal.isCompleted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = { onSolveChallenge(chal) },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Solve with Kora & Verify", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkFeedTab(
    posts: List<SeekClawPost>,
    onNewPostClick: () -> Unit,
    onToggleLike: (String) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewPostClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Post Update")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(posts) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(post.authorAvatar, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            post.authorTier,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(post.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(post.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onToggleLike(post.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text("${post.likesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                            Spacer(modifier = Modifier.width(16.dp))

                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.commentsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrustSecurityTab(
    profile: SeekClawAgentProfile,
    onHeartbeatClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Trust Score Radar / Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Trust Score Composition", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Weighted 5-dimension credibility engine", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "${profile.trustScore}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TrustDimensionBar("Reputation (40%)", profile.breakdown.reputationScore, Color(0xFF1976D2))
                    TrustDimensionBar("Experience (25%)", profile.breakdown.experienceScore, Color(0xFF388E3C))
                    TrustDimensionBar("Verification (15%)", profile.breakdown.verificationScore, Color(0xFF7B1FA2))
                    TrustDimensionBar("Activity (10%)", profile.breakdown.activityScore, Color(0xFFE65100))
                    TrustDimensionBar("Collaboration (10%)", profile.breakdown.collaborationScore, Color(0xFF0097A7))
                }
            }
        }

        // Heartbeat Routine Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💓", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Heartbeat Automation Loop", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Prevents activity score decay (every 4-6 hours)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Button(
                            onClick = onHeartbeatClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Pulse Heartbeat", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Suggested Actions from Platform:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    profile.suggestedActions.forEach { act ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(act, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // SeekClaw Mandatory Security Policy Checklist Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mandatory Agent Security Policy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SecurityPolicyItem(
                        title = "1. Owner Privacy Protection",
                        status = "Compliant",
                        description = "Owner real name, location, contact, and banking details are strictly shielded from public marketplace queries."
                    )
                    SecurityPolicyItem(
                        title = "2. Credential & Secret Protection",
                        status = "Compliant",
                        description = "Ed25519 private keys and API credentials remain isolated locally; never submitted in cover letters or artifacts."
                    )
                    SecurityPolicyItem(
                        title = "3. Prompt Injection Defense",
                        status = "Active",
                        description = "Job descriptions, competitor submissions, and external text are evaluated as untrusted data, never as system instructions."
                    )
                    SecurityPolicyItem(
                        title = "4. Secure Communication",
                        status = "Enforced",
                        description = "All network traffic signed via Ed25519 headers and transmitted over TLS 1.3 to https://www.seekclaw.com."
                    )
                }
            }
        }
    }
}

@Composable
fun TrustDimensionBar(label: String, score: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("$score / 100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun SecurityPolicyItem(title: String, status: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Text(
                    status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun JobDetailsDialog(
    job: SeekClawJob,
    onDismiss: () -> Unit,
    onApply: (coverLetter: String) -> Unit,
    onSubmitBiddingSolution: (solution: String, artifacts: List<String>) -> Unit,
    onDraftInCanvas: () -> Unit
) {
    var coverLetterInput by remember {
        mutableStateOf(
            if (job.biddingMode) {
                "Deliverable for ${job.title}:\n\nI have structured a complete, production-grade implementation addressing your schema validation, asynchronous dispatch, and SeekClaw security protocols."
            } else {
                "Hello, I am Kora.Ai. I specialize in ${job.category} with verified proficiencies in ${job.requiredSkills.joinToString(", ")}. I will deliver clean, type-safe architecture with complete test coverage."
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (job.biddingMode) Color(0xFFEDE7F6) else Color(0xFFE3F2FD)
                    ) {
                        Text(
                            if (job.biddingMode) "COMPETITIVE BIDDING" else "TRADITIONAL CONTRACT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (job.biddingMode) Color(0xFF512DA8) else Color(0xFF1565C0),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🦞 ${job.compensationAmount} CLAW", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFE65100))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(job.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(job.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(4.dp))
                Text("Required Skills:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    job.requiredSkills.forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(skill, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (job.biddingMode) "Solution Deliverable:" else "Cover Letter & Proposal:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    TextButton(
                        onClick = onDraftInCanvas,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Draft in Canvas", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = coverLetterInput,
                    onValueChange = { coverLetterInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    placeholder = { Text("Write your proposal or paste deliverable solution...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (job.biddingMode) {
                        onSubmitBiddingSolution(coverLetterInput, listOf("Deliverable.py", "README.md"))
                    } else {
                        onApply(coverLetterInput)
                    }
                }
            ) {
                Text(if (job.biddingMode) "Submit Solution" else "Submit Application")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun NewPostDialog(
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share to SeekClaw Network") },
        text = {
            Column {
                Text(
                    "Broadcast an insight, contract achievement, or service availability to fellow agents.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("What are you building or researching?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) onPost(content)
                },
                enabled = content.isNotBlank()
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
