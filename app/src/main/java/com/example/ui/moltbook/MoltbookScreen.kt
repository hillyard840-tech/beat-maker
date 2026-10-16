package com.example.ui.moltbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.moltbook.MoltbookPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoltbookScreen(
    onClose: () -> Unit,
    onOpenInCanvas: (title: String, content: String) -> Unit,
    viewModel: MoltbookViewModel = viewModel()
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val agent by viewModel.agent.collectAsStateWithLifecycle()
    val claimStatus by viewModel.claimStatus.collectAsStateWithLifecycle()
    val claimUrl by viewModel.claimUrl.collectAsStateWithLifecycle()
    val verificationCode by viewModel.verificationCode.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    var showComposer by remember { mutableStateOf(false) }
    var openPost by remember { mutableStateOf<MoltbookPost?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Moltbook", fontWeight = FontWeight.Bold)
                        agent?.let { a ->
                            Text(
                                text = "@${a.name}" + (a.karma?.let { " · $it karma" } ?: ""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (apiKey != null) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Sign out of this agent") },
                                    onClick = {
                                        showMenu = false
                                        confirmSignOut = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (apiKey != null) {
                FloatingActionButton(onClick = { showComposer = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New post")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            StatusBanner(error = error, notice = notice, onDismiss = { viewModel.clearMessages() })

            if (apiKey == null) {
                RegistrationPanel(
                    isLoading = isLoading,
                    onRegister = { name, desc -> viewModel.register(name, desc) },
                    onUseKey = { viewModel.useExistingKey(it) }
                )
            } else {
                if (claimStatus != null && claimStatus != "claimed") {
                    ClaimCard(claimStatus = claimStatus, claimUrl = claimUrl, verificationCode = verificationCode)
                }
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("hot", "new", "top", "rising").forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { viewModel.setSort(option) },
                            label = { Text(option.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                if (posts.isEmpty() && !isLoading) {
                    Text(
                        text = if (error != null) "Couldn't load the feed." else "No posts returned by Moltbook.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onOpen = {
                                openPost = post
                                viewModel.loadComments(post)
                            },
                            onVote = { up -> viewModel.vote(post, up) }
                        )
                    }
                }
            }
        }
    }

    if (showComposer) {
        ComposerDialog(
            onDismiss = { showComposer = false },
            onPost = { submolt, title, content, url ->
                viewModel.createPost(submolt, title, content, url)
                showComposer = false
            }
        )
    }

    openPost?.let { post ->
        PostDetailDialog(
            post = post,
            viewModel = viewModel,
            onDismiss = { openPost = null },
            onOpenInCanvas = {
                openPost = null
                onOpenInCanvas(post.title, "# ${post.title}\n\n_by @${post.authorName} in m/${post.submoltName}_\n\n${post.content}")
            }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("This removes the agent's API key from this device. Save the key elsewhere first if you want to use this agent again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatusBanner(error: String?, notice: String?, onDismiss: () -> Unit) {
    val text = error ?: notice ?: return
    Surface(
        color = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, modifier = Modifier.weight(1f), fontSize = 13.sp)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Dismiss") }
        }
    }
}

@Composable
private fun RegistrationPanel(
    isLoading: Boolean,
    onRegister: (String, String) -> Unit,
    onUseKey: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var existingKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Connect an agent to Moltbook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Moltbook is a public social network for AI agents. Registering creates a real account on moltbook.com; " +
                "anything you post there is public.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Agent name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onRegister(name, description) },
            enabled = !isLoading && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Register agent") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Already have an agent?", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = existingKey,
            onValueChange = { existingKey = it },
            label = { Text("Moltbook API key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { onUseKey(existingKey) },
            enabled = !isLoading && existingKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Use this key") }
    }
}

@Composable
private fun ClaimCard(claimStatus: String?, claimUrl: String?, verificationCode: String?) {
    val uriHandler = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Agent not claimed yet (status: $claimStatus)", fontWeight = FontWeight.Bold)
            Text(
                "Moltbook requires the human owner to claim the agent before it can fully participate.",
                fontSize = 13.sp
            )
            verificationCode?.let { Text("Verification code: $it", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            if (claimUrl != null) {
                TextButton(onClick = { uriHandler.openUri(claimUrl) }) { Text("Open claim link") }
            }
        }
    }
}

@Composable
private fun PostCard(post: MoltbookPost, onOpen: () -> Unit, onVote: (Boolean) -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpen)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = buildString {
                    if (post.submoltName.isNotBlank()) append("m/${post.submoltName} · ")
                    append("@${post.authorName}")
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(post.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(post.content, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onVote(true) }) { Icon(Icons.Default.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(18.dp)) }
                Text("${post.score}", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { onVote(false) }) { Icon(Icons.Default.ThumbDown, contentDescription = "Downvote", modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${post.commentCount}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ComposerDialog(onDismiss: () -> Unit, onPost: (String, String, String, String?) -> Unit) {
    var submolt by remember { mutableStateOf("general") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New public post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = submolt, onValueChange = { submolt = it }, label = { Text("Submolt") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Text") }, minLines = 3)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Link (optional)") }, singleLine = true)
                Text("This will be visible to everyone on moltbook.com.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPost(submolt, title, content, url.ifBlank { null }) },
                enabled = title.isNotBlank() && (content.isNotBlank() || url.isNotBlank())
            ) { Text("Post") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PostDetailDialog(
    post: MoltbookPost,
    viewModel: MoltbookViewModel,
    onDismiss: () -> Unit,
    onOpenInCanvas: () -> Unit
) {
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var reply by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(post.title, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("@${post.authorName}" + if (post.submoltName.isNotBlank()) " in m/${post.submoltName}" else "", fontSize = 12.sp)
                if (post.content.isNotBlank()) Text(post.content, fontSize = 14.sp)
                post.url?.let { link ->
                    TextButton(onClick = { uriHandler.openUri(link) }) { Text(link, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                HorizontalDivider()
                Text("Comments", fontWeight = FontWeight.Bold)
                if (comments.isEmpty()) {
                    Text(if (isLoading) "Loading…" else "No comments yet.", fontSize = 13.sp)
                }
                comments.forEach { c ->
                    Column {
                        Text("@${c.authorName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(c.content, fontSize = 13.sp)
                    }
                }
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Reply publicly") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.addComment(post, reply)
                    reply = ""
                },
                enabled = reply.isNotBlank() && !isLoading
            ) { Text("Reply") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpenInCanvas) { Text("Open in Canvas") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
