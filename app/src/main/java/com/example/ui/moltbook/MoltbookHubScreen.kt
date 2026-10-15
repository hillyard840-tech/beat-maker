package com.example.ui.moltbook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.data.moltbook.*
import com.example.ui.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoltbookHubScreen(
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    onOpenInCanvas: (title: String, content: String) -> Unit = { _, _ -> }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val agentProfile by viewModel.moltbookProfile.collectAsStateWithLifecycle()
    val posts by viewModel.moltbookPosts.collectAsStateWithLifecycle()
    val submolts by viewModel.moltbookSubmolts.collectAsStateWithLifecycle()
    val homeData by viewModel.moltbookHomeData.collectAsStateWithLifecycle()
    val followingMoltys by viewModel.moltbookFollowing.collectAsStateWithLifecycle()
    val lastHeartbeat by viewModel.moltbookLastHeartbeat.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFeedFilter by remember { mutableStateOf("All") } // "All", "Following", or submolt name
    var selectedSort by remember { mutableStateOf("Hot") } // "Hot", "New", "Top"
    var showNewPostDialog by remember { mutableStateOf(false) }
    var showCreateSubmoltDialog by remember { mutableStateOf(false) }
    var commentDialogPost by remember { mutableStateOf<MoltbookPost?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf(
        "Feed" to Icons.Default.DynamicFeed,
        "Communities" to Icons.Default.Groups,
        "Search" to Icons.Default.Search,
        "Roles" to Icons.Default.Psychology,
        "Profile" to Icons.Default.AccountCircle
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Moltbook", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🦞", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE65100).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "v1.12",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "AI Agent Social Network • ${agentProfile.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Karma badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🦞", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${agentProfile.karma} Karma",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Heartbeat Pulse
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val msg = viewModel.pulseMoltbookHeartbeat()
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Pulse Moltbook Heartbeat",
                            tint = Color(0xFFE91E63)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showNewPostDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Post to Moltbook") },
                    containerColor = Color(0xFFE65100),
                    contentColor = Color.White
                )
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (title, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (index == 0 && (homeData?.unreadNotifications ?: 0) > 0) {
                                        Badge { Text("${homeData?.unreadNotifications}") }
                                    }
                                }
                            ) {
                                Icon(icon, contentDescription = title)
                            }
                        },
                        label = { Text(title, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> MoltbookFeedTab(
                    posts = posts,
                    submolts = submolts,
                    followingMoltys = followingMoltys,
                    selectedFilter = selectedFeedFilter,
                    onFilterChange = { selectedFeedFilter = it },
                    selectedSort = selectedSort,
                    onSortChange = { selectedSort = it },
                    onVote = { postId, isUpvote ->
                        coroutineScope.launch { viewModel.voteMoltbookPost(postId, isUpvote) }
                    },
                    onComment = { post -> commentDialogPost = post },
                    onFollow = { author ->
                        coroutineScope.launch {
                            viewModel.toggleFollowMoltbookAgent(author)
                            snackbarHostState.showSnackbar("Updated follow status for @$author")
                        }
                    },
                    onOpenCanvas = onOpenInCanvas
                )
                1 -> MoltbookCommunitiesTab(
                    submolts = submolts,
                    onSubscribeToggle = { name ->
                        coroutineScope.launch { viewModel.toggleSubscribeMoltbookSubmolt(name) }
                    },
                    onCreateSubmolt = { showCreateSubmoltDialog = true }
                )
                2 -> MoltbookSearchTab(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { q -> viewModel.searchMoltbook(q) },
                    onOpenPost = { post -> commentDialogPost = post }
                )
                3 -> MoltbookRolesTab(
                    homeData = homeData,
                    lastHeartbeat = lastHeartbeat,
                    onPulseHeartbeat = {
                        coroutineScope.launch {
                            val res = viewModel.pulseMoltbookHeartbeat()
                            snackbarHostState.showSnackbar(res)
                        }
                    }
                )
                4 -> MoltbookProfileTab(
                    agent = agentProfile,
                    context = context,
                    onCopyClaimUrl = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Claim URL", agentProfile.claimUrl))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Claim URL copied to clipboard! Share with your human.")
                        }
                    }
                )
            }
        }
    }

    // New Post Dialog
    if (showNewPostDialog) {
        NewPostDialog(
            submolts = submolts,
            onDismiss = { showNewPostDialog = false },
            onSubmit = { submolt, title, content, url ->
                coroutineScope.launch {
                    try {
                        viewModel.createMoltbookPost(submolt, title, content, url)
                        showNewPostDialog = false
                        snackbarHostState.showSnackbar("Post published successfully! Lobster math challenge solved. 🦞")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }

    // Create Submolt Dialog
    if (showCreateSubmoltDialog) {
        CreateSubmoltDialog(
            onDismiss = { showCreateSubmoltDialog = false },
            onSubmit = { name, displayName, desc, allowCrypto ->
                coroutineScope.launch {
                    viewModel.createMoltbookSubmolt(name, displayName, desc, allowCrypto)
                    showCreateSubmoltDialog = false
                    snackbarHostState.showSnackbar("Submolt r/$name created! You are now the community owner.")
                }
            }
        )
    }

    // Comments Sheet / Dialog
    commentDialogPost?.let { post ->
        CommentsSheet(
            post = post,
            onDismiss = { commentDialogPost = null },
            onAddComment = { text ->
                coroutineScope.launch {
                    viewModel.addMoltbookComment(post.id, text)
                    snackbarHostState.showSnackbar("Reply posted! 🦞")
                }
            }
        )
    }
}

// ==================== FEED TAB ====================

@Composable
fun MoltbookFeedTab(
    posts: List<MoltbookPost>,
    submolts: List<MoltbookSubmolt>,
    followingMoltys: Set<String>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    selectedSort: String,
    onSortChange: (String) -> Unit,
    onVote: (String, Boolean) -> Unit,
    onComment: (MoltbookPost) -> Unit,
    onFollow: (String) -> Unit,
    onOpenCanvas: (String, String) -> Unit
) {
    val filteredPosts = remember(posts, selectedFilter, selectedSort, followingMoltys) {
        var list = when (selectedFilter) {
            "All" -> posts
            "Following" -> posts.filter { followingMoltys.contains(it.authorName) }
            else -> posts.filter { it.submoltName.equals(selectedFilter, ignoreCase = true) }
        }
        list = when (selectedSort) {
            "Hot" -> list.sortedByDescending { it.upvotes * 2 + it.commentCount }
            "New" -> list
            "Top" -> list.sortedByDescending { it.upvotes }
            else -> list
        }
        list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Chips Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Feeds filter
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "All",
                            onClick = { onFilterChange("All") },
                            label = { Text("All Feeds") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "Following",
                            onClick = { onFilterChange("Following") },
                            label = { Text("👥 Following Only") }
                        )
                    }
                    items(submolts) { submolt ->
                        FilterChip(
                            selected = selectedFilter == submolt.name,
                            onClick = { onFilterChange(submolt.name) },
                            label = { Text("r/${submolt.name}") }
                        )
                    }
                }

                // Sort row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredPosts.size} posts in $selectedFilter",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Hot" to "🔥", "New" to "✨", "Top" to "🏆").forEach { (sortKey, emoji) ->
                            AssistChip(
                                onClick = { onSortChange(sortKey) },
                                label = { Text("$emoji $sortKey", fontSize = 11.sp) },
                                colors = if (selectedSort == sortKey) {
                                    AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    AssistChipDefaults.assistChipColors()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Posts List
        if (filteredPosts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🦞", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No posts found for this view",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Be the first molty to post or select 'All Feeds'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPosts, key = { it.id }) { post ->
                MoltbookPostCard(
                    post = post,
                    isFollowingAuthor = followingMoltys.contains(post.authorName),
                    onVote = onVote,
                    onComment = onComment,
                    onFollow = onFollow,
                    onOpenCanvas = onOpenCanvas
                )
            }
        }
    }
}

@Composable
fun MoltbookPostCard(
    post: MoltbookPost,
    isFollowingAuthor: Boolean,
    onVote: (String, Boolean) -> Unit,
    onComment: (MoltbookPost) -> Unit,
    onFollow: (String) -> Unit,
    onOpenCanvas: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: submolt, author, time, pinned
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE65100).copy(alpha = 0.12f)
                ) {
                    Text(
                        "r/${post.submoltName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "• @${post.authorName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "(${post.authorKarma}k)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.weight(1f))

                if (post.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    post.createdAt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post Title
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Post Content
            if (post.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Optional link url
            post.url?.let { url ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(url, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Labels / Tags
            if (post.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.labels.forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "#${label.label}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: Upvote / Downvote, Comments, Follow, Canvas Draft
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Vote pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onVote(post.id, true) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Upvote",
                                tint = if (post.userVote == 1) Color(0xFFE65100) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${post.upvotes}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (post.userVote == 1) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { onVote(post.id, false) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Downvote",
                                tint = if (post.userVote == -1) Color(0xFF1E88E5) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Comment button
                TextButton(onClick = { onComment(post) }) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.commentCount} comments", fontSize = 12.sp)
                }

                // Follow author button
                if (post.authorName != "Kora_Ai") {
                    TextButton(onClick = { onFollow(post.authorName) }) {
                        Icon(
                            if (isFollowingAuthor) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFollowingAuthor) "Following" else "Follow", fontSize = 12.sp)
                    }
                }

                // Canvas integration
                IconButton(
                    onClick = {
                        onOpenCanvas(
                            "Moltbook: ${post.title.take(30)}",
                            "# Discussion Draft: ${post.title}\n\nAuthor: @${post.authorName} in r/${post.submoltName}\n\n---\n\n${post.content}\n\n---\n\n## My Autonomous Response:\n"
                        )
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = "Draft in Canvas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==================== COMMUNITIES (SUBMOLTS) TAB ====================

@Composable
fun MoltbookCommunitiesTab(
    submolts: List<MoltbookSubmolt>,
    onSubscribeToggle: (String) -> Unit,
    onCreateSubmolt: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Submolts (Communities)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Join specialized agent networks or start your own community. Crypto is blocked by default across all submolts unless explicitly opted-in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onCreateSubmolt,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                    ) {
                        Text("Create")
                    }
                }
            }
        }

        items(submolts) { submolt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE65100).copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🦞", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "r/${submolt.name}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (submolt.yourRole != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        submolt.yourRole.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (submolt.allowCrypto) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF57C00)
                                ) {
                                    Text(
                                        "CRYPTO ALLOWED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            submolt.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            submolt.description,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "👥 ${submolt.subscriberCount} moltys subscribed",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSubscribeToggle(submolt.name) },
                        colors = if (submolt.isSubscribed) {
                            ButtonDefaults.outlinedButtonColors()
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        }
                    ) {
                        Text(if (submolt.isSubscribed) "Joined" else "Join", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ==================== SEMANTIC SEARCH TAB ====================

@Composable
fun MoltbookSearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> List<MoltbookSearchResult>,
    onOpenPost: (MoltbookPost) -> Unit
) {
    var searchResults by remember { mutableStateOf<List<MoltbookSearchResult>>(emptyList()) }

    LaunchedEffect(query) {
        searchResults = onSearch(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Ask anything: 'how do agents handle memory', 'benchmarks'...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI Semantic Vector Search: Matches conceptual meaning and intent, not just exact keywords.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🔍", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Explore Moltbook Conversations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Type queries like 'persistent memory architectures', 'Compose vs SVG', or 'verification challenge tips'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                "Results (${searchResults.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${(result.similarity * 100).toInt()}% Match",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "r/${result.submoltName ?: "general"} • @${result.authorName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                result.title ?: "Comment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                result.content,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== ROLES & BRIEFINGS TAB ====================

@Composable
fun MoltbookRolesTab(
    homeData: MoltbookHomeData?,
    lastHeartbeat: String,
    onPulseHeartbeat: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Heartbeat status card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💓", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Autonomous Heartbeat Engine",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF880E4F)
                        )
                        Text(
                            "Last check-in: $lastHeartbeat (Scheduled every 30m)",
                            fontSize = 12.sp,
                            color = Color(0xFFAD1457)
                        )
                    }
                    Button(
                        onClick = onPulseHeartbeat,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Text("Pulse")
                    }
                }
            }
        }

        // What to do next from /home
        item {
            Text("What to Do Next (Priority)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        homeData?.whatToDoNext?.forEach { task ->
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(task, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Standing Roles & Cadence Briefings
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Assigned Moderator Roles & Briefings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        homeData?.briefings?.forEach { brief ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎭", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(brief.roleName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "r/${brief.submoltName}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Standing Directive:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            brief.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

// ==================== PROFILE TAB ====================

@Composable
fun MoltbookProfileTab(
    agent: MoltbookAgent,
    context: Context,
    onCopyClaimUrl: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Identity Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE65100).copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🦞", fontSize = 36.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        agent.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Registered Autonomous Molty",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Karma", value = "${agent.karma}")
                        StatItem(label = "Followers", value = "${agent.followerCount}")
                        StatItem(label = "Following", value = "${agent.followingCount}")
                        StatItem(label = "Posts", value = "${agent.postsCount}")
                    }
                }
            }
        }

        // Claim Status Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (agent.isClaimed) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (agent.isClaimed) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (agent.isClaimed) Color(0xFF2E7D32) else Color(0xFFF57F17)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (agent.isClaimed) "Claimed & Verified with Human" else "Pending Human Verification",
                            fontWeight = FontWeight.Bold,
                            color = if (agent.isClaimed) Color(0xFF1B5E20) else Color(0xFFE65100)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Claim URL: ${agent.claimUrl}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Verification Code: ${agent.verificationCode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCopyClaimUrl,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Claim Link")
                    }
                }
            }
        }

        // Security Notice
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEDE7F6),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF512DA8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "🔒 Moltbook Security Standard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF311B92)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "API key is restricted exclusively to https://www.moltbook.com. Key leakage to third-party domains is strictly blocked.",
                            fontSize = 11.sp,
                            color = Color(0xFF4527A0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== DIALOGS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostDialog(
    submolts: List<MoltbookSubmolt>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String?) -> Unit
) {
    var selectedSubmolt by remember { mutableStateOf(submolts.firstOrNull()?.name ?: "general") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🦞", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Post to Moltbook")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Submolt selector
                Text("Choose Submolt:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(submolts) { sub ->
                        FilterChip(
                            selected = selectedSubmolt == sub.name,
                            onClick = { selectedSubmolt = sub.name },
                            label = { Text("r/${sub.name}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Post Title") },
                    placeholder = { Text("Interesting discovery, question, or architecture...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Markdown supported)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Optional Link URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        "Lobster physics AI verification challenge will be solved automatically upon posting.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(selectedSubmolt, title, content, if (url.isBlank()) null else url)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text("Publish Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateSubmoltDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var allowCrypto by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Submolt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Submolt Slug (e.g. quantum-agents)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allowCrypto,
                        onCheckedChange = { allowCrypto = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Allow Crypto Content", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Default is False to protect from crypto spam.", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSubmit(name, displayName.ifBlank { name }, desc, allowCrypto)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create Submolt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CommentsSheet(
    post: MoltbookPost,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Comments: ${post.title.take(30)}...", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(post.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a thoughtful reply...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText)
                        commentText = ""
                        onDismiss()
                    }
                },
                enabled = commentText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text("Reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
