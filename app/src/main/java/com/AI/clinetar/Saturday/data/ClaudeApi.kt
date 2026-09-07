package com.AI.clinetar.Saturday.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal data model for the Anthropic Messages API (`POST /v1/messages`).
 * See https://docs.claude.com/en/api/messages
 */

@Serializable
data class MessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ApiMessage>,
    val system: String? = null,
)

@Serializable
data class ApiMessage(
    /** "user" or "assistant" */
    val role: String,
    val content: String,
)

@Serializable
data class MessagesResponse(
    val id: String? = null,
    val role: String? = null,
    val model: String? = null,
    val content: List<ContentBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: Usage? = null,
) {
    /** Concatenated text of every `text` block in the reply. */
    fun text(): String =
        content.filter { it.type == "text" }.joinToString("") { it.text.orEmpty() }.trim()

    /** Total tokens billed for this turn (input + output + cache). */
    fun totalTokens(): Int = usage?.total() ?: 0
}

@Serializable
data class Usage(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
    @SerialName("cache_creation_input_tokens") val cacheCreationInputTokens: Int = 0,
    @SerialName("cache_read_input_tokens") val cacheReadInputTokens: Int = 0,
) {
    fun total(): Int = inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens
}

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null,
)

@Serializable
data class ApiError(
    val type: String? = null,
    val error: ApiErrorBody? = null,
)

@Serializable
data class ApiErrorBody(
    val type: String? = null,
    val message: String? = null,
)
