package com.AI.clinetar.Saturday.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Result of one call to Claude. */
sealed interface ChatResult {
    data class Success(val reply: String) : ChatResult
    data class Failure(val message: String) : ChatResult
}

/**
 * Talks to the Anthropic Messages API directly from the device.
 *
 * The whole conversation is re-sent on every turn – Claude is stateless, so the
 * client is responsible for keeping history.
 */
class ClaudeRepository(
    private val settings: SettingsStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun send(
        history: List<ApiMessage>,
        systemPrompt: String? = SYSTEM_PROMPT,
    ): ChatResult = withContext(Dispatchers.IO) {
        val apiKey = settings.apiKey
        if (apiKey.isBlank()) {
            return@withContext ChatResult.Failure(
                "No API key set. Add your Anthropic API key in Settings.",
            )
        }

        val payload = MessagesRequest(
            model = settings.model,
            maxTokens = MAX_TOKENS,
            messages = history,
            system = systemPrompt,
        )

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .post(json.encodeToString(MessagesRequest.serializer(), payload).toRequestBody(JSON_MEDIA))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                captureRateLimit(response)
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val parsed = json.decodeFromString(MessagesResponse.serializer(), body)
                    val text = parsed.text()
                    if (text.isEmpty()) {
                        ChatResult.Failure("Claude returned an empty response.")
                    } else {
                        settings.recordUsage(payload.model, parsed.totalTokens())
                        ChatResult.Success(text)
                    }
                } else {
                    ChatResult.Failure(readableError(response.code, body))
                }
            }
        } catch (e: IOException) {
            ChatResult.Failure("Network error: ${e.message ?: "could not reach the API"}")
        } catch (e: Exception) {
            ChatResult.Failure("Unexpected error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Pull the account token rate-limit budget out of the response headers
     * (`anthropic-ratelimit-tokens-*`, falling back to the input-token variant)
     * and stash it for the Settings screen.
     */
    private fun captureRateLimit(response: Response) {
        val h = response.headers
        fun long(name: String) = h[name]?.trim()?.toLongOrNull()

        val limit = long("anthropic-ratelimit-tokens-limit")
            ?: long("anthropic-ratelimit-input-tokens-limit")
        val remaining = long("anthropic-ratelimit-tokens-remaining")
            ?: long("anthropic-ratelimit-input-tokens-remaining")
        val reset = h["anthropic-ratelimit-tokens-reset"]
            ?: h["anthropic-ratelimit-input-tokens-reset"]
            ?: ""

        if (limit != null && remaining != null) {
            settings.saveRateLimit(limit, remaining, reset)
        }
    }

    private fun readableError(code: Int, body: String): String {
        val apiMessage = runCatching {
            json.decodeFromString(ApiError.serializer(), body).error?.message
        }.getOrNull()
        val hint = when (code) {
            401 -> "check that your API key is correct"
            403 -> "your API key doesn't have access to this model"
            404 -> "unknown model id – check the model in Settings"
            429 -> "rate limited – wait a moment and try again"
            in 500..599 -> "Anthropic server error – try again shortly"
            else -> null
        }
        return buildString {
            append("API error $code")
            if (apiMessage != null) append(": $apiMessage")
            if (hint != null) append(" ($hint)")
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val MAX_TOKENS = 4096
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        const val SYSTEM_PROMPT =
            "You are a helpful assistant inside a small Android chat app called Saturday. " +
                "Keep replies clear and reasonably concise."
    }
}
