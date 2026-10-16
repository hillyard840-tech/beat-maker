package com.example.ui.moltbook

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.moltbook.MoltbookAgent
import com.example.data.moltbook.MoltbookClient
import com.example.data.moltbook.MoltbookComment
import com.example.data.moltbook.MoltbookPost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State for the real Moltbook integration. Every value shown comes from moltbook.com. */
class MoltbookViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API_KEY, null))
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    private val _claimUrl = MutableStateFlow(prefs.getString(KEY_CLAIM_URL, null))
    val claimUrl: StateFlow<String?> = _claimUrl.asStateFlow()

    private val _verificationCode = MutableStateFlow(prefs.getString(KEY_VERIFICATION_CODE, null))
    val verificationCode: StateFlow<String?> = _verificationCode.asStateFlow()

    private val _claimStatus = MutableStateFlow<String?>(null)
    val claimStatus: StateFlow<String?> = _claimStatus.asStateFlow()

    private val _agent = MutableStateFlow<MoltbookAgent?>(null)
    val agent: StateFlow<MoltbookAgent?> = _agent.asStateFlow()

    private val _posts = MutableStateFlow<List<MoltbookPost>>(emptyList())
    val posts: StateFlow<List<MoltbookPost>> = _posts.asStateFlow()

    private val _sort = MutableStateFlow("hot")
    val sort: StateFlow<String> = _sort.asStateFlow()

    private val _comments = MutableStateFlow<List<MoltbookComment>>(emptyList())
    val comments: StateFlow<List<MoltbookComment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    init {
        if (_apiKey.value != null) refresh()
    }

    fun clearMessages() {
        _error.value = null
        _notice.value = null
    }

    private fun request(block: suspend (String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                block(_apiKey.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: e.javaClass.simpleName
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, description: String) = request {
        val reg = MoltbookClient.register(name.trim(), description.trim())
        saveCredentials(reg.apiKey, reg.claimUrl, reg.verificationCode)
        _notice.value = "Agent registered. Open the claim link to verify ownership before posting."
        loadAll(reg.apiKey)
    }

    /** For users who already registered an agent elsewhere. */
    fun useExistingKey(key: String) = request {
        val trimmed = key.trim()
        // Validate against the server before saving.
        val me = MoltbookClient.me(trimmed) ?: throw IllegalStateException("That API key was not accepted by Moltbook")
        saveCredentials(trimmed, null, null)
        _agent.value = me
        loadAll(trimmed)
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _apiKey.value = null
        _claimUrl.value = null
        _verificationCode.value = null
        _claimStatus.value = null
        _agent.value = null
        _posts.value = emptyList()
        _comments.value = emptyList()
    }

    fun refresh() = request { key -> if (key != null) loadAll(key) }

    fun setSort(sort: String) {
        _sort.value = sort
        request { key -> if (key != null) _posts.value = MoltbookClient.feed(key, sort) }
    }

    fun createPost(submolt: String, title: String, content: String, url: String?) = request { key ->
        requireNotNull(key) { "Register an agent first" }
        MoltbookClient.createPost(key, submolt.trim().ifBlank { "general" }, title.trim(), content.trim(), url?.trim())
        _notice.value = "Posted to m/${submolt.ifBlank { "general" }}"
        _posts.value = MoltbookClient.feed(key, _sort.value)
    }

    fun vote(post: MoltbookPost, upvote: Boolean) = request { key ->
        requireNotNull(key) { "Register an agent first" }
        MoltbookClient.vote(key, post.id, upvote)
        _posts.value = MoltbookClient.feed(key, _sort.value)
    }

    fun loadComments(post: MoltbookPost) {
        _comments.value = emptyList()
        request { key -> if (key != null) _comments.value = MoltbookClient.comments(key, post.id) }
    }

    fun addComment(post: MoltbookPost, content: String) = request { key ->
        requireNotNull(key) { "Register an agent first" }
        MoltbookClient.addComment(key, post.id, content.trim())
        _comments.value = MoltbookClient.comments(key, post.id)
    }

    private suspend fun loadAll(key: String) {
        _claimStatus.value = MoltbookClient.claimStatus(key)
        _agent.value = MoltbookClient.me(key) ?: _agent.value
        _posts.value = MoltbookClient.feed(key, _sort.value)
    }

    private fun saveCredentials(key: String, claimUrl: String?, code: String?) {
        prefs.edit()
            .putString(KEY_API_KEY, key)
            .putString(KEY_CLAIM_URL, claimUrl)
            .putString(KEY_VERIFICATION_CODE, code)
            .apply()
        _apiKey.value = key
        _claimUrl.value = claimUrl
        _verificationCode.value = code
    }

    companion object {
        /** Separate from the old simulated "moltbook_agent_prefs" file, which held a fake key. */
        const val PREFS_NAME = "moltbook_credentials"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CLAIM_URL = "claim_url"
        private const val KEY_VERIFICATION_CODE = "verification_code"
    }
}
