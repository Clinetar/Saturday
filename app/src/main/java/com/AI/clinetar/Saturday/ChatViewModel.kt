package com.AI.clinetar.Saturday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.AI.clinetar.Saturday.data.ApiMessage
import com.AI.clinetar.Saturday.data.ChatResult
import com.AI.clinetar.Saturday.data.ClaudeModels
import com.AI.clinetar.Saturday.data.ClaudeRepository
import com.AI.clinetar.Saturday.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Author { USER, CLAUDE }

data class ChatMessage(
    val id: Long,
    val author: Author,
    val text: String,
    val isError: Boolean = false,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val hasApiKey: Boolean = false,
    val modelLabel: String = "",
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)
    private val repository = ClaudeRepository(settings)

    private val _state = MutableStateFlow(
        ChatUiState(
            hasApiKey = settings.hasApiKey,
            modelLabel = ClaudeModels.labelFor(settings.model),
        ),
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var nextId = 0L

    /** Re-read settings (called when returning from the Settings screen). */
    fun refreshSettings() {
        _state.update {
            it.copy(
                hasApiKey = settings.hasApiKey,
                modelLabel = ClaudeModels.labelFor(settings.model),
            )
        }
    }

    fun send(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty() || _state.value.isSending) return

        val userMessage = ChatMessage(nextId++, Author.USER, input)
        _state.update { it.copy(messages = it.messages + userMessage, isSending = true) }

        viewModelScope.launch {
            val history = _state.value.messages
                .filterNot { it.isError }
                .map { ApiMessage(role = if (it.author == Author.USER) "user" else "assistant", content = it.text) }

            val reply = when (val result = repository.send(history)) {
                is ChatResult.Success -> ChatMessage(nextId++, Author.CLAUDE, result.reply)
                is ChatResult.Failure -> ChatMessage(nextId++, Author.CLAUDE, result.message, isError = true)
            }

            _state.update {
                it.copy(messages = it.messages + reply, isSending = false, hasApiKey = settings.hasApiKey)
            }
        }
    }

    fun clearConversation() {
        _state.update { it.copy(messages = emptyList()) }
    }
}
