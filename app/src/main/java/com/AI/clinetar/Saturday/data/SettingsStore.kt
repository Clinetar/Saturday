package com.AI.clinetar.Saturday.data

import android.content.Context
import androidx.core.content.edit

/**
 * The last token rate-limit snapshot seen in an API response header
 * (`anthropic-ratelimit-tokens-*`). This is the per-minute token budget the
 * account shares across keys — the closest thing the Messages API exposes to an
 * "account-wide" usage/remaining number.
 */
data class RateLimit(
    val limit: Long,
    val remaining: Long,
    val resetIso: String,
) {
    val used: Long get() = (limit - remaining).coerceAtLeast(0L)
    val usedFraction: Float get() = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
}

/**
 * Tiny wrapper over [android.content.SharedPreferences]: the Anthropic API key,
 * the chosen model, and per-model usage stats (message count + total tokens)
 * that drive the model dropdown order and the usage meter in Settings.
 *
 * NOTE: SharedPreferences is private to the app but stored in plain text on the
 * device. Fine for a personal/demo build; a production app should keep the key
 * on a backend and never ship it to the client.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("saturday_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_API_KEY, value.trim()) }

    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        set(value) = prefs.edit { putString(KEY_MODEL, value.trim()) }

    /** Which screen the app opens on: "text" or "voice". */
    var defaultScreen: String
        get() = prefs.getString(KEY_DEFAULT_SCREEN, SCREEN_TEXT).orEmpty().ifBlank { SCREEN_TEXT }
        set(value) = prefs.edit { putString(KEY_DEFAULT_SCREEN, value) }

    /** Selected text-to-speech tone id (see [com.AI.clinetar.Saturday.data.VoiceTones]). */
    var voiceTone: String
        get() = prefs.getString(KEY_VOICE_TONE, VoiceTones.DEFAULT_ID).orEmpty().ifBlank { VoiceTones.DEFAULT_ID }
        set(value) = prefs.edit { putString(KEY_VOICE_TONE, value) }

    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    /**
     * What the UI is allowed to show once a key is stored: dots plus the last 4
     * characters. The real key never leaves this class after it's saved.
     */
    val apiKeyMasked: String
        get() = apiKey.let { if (it.length >= 4) "•••• •••• •••• " + it.takeLast(4) else "" }

    // ---- per-model usage ----

    fun tokensUsed(model: String): Long = prefs.getLong(tokensKey(model), 0L)

    fun sendCount(model: String): Int = prefs.getInt(countKey(model), 0)

    /** Record one successful exchange with [model]. */
    fun recordUsage(model: String, tokens: Int) = prefs.edit {
        putLong(tokensKey(model), tokensUsed(model) + tokens.coerceAtLeast(0))
        putInt(countKey(model), sendCount(model) + 1)
    }

    /**
     * The known model ids ordered by how many messages have been sent with each
     * (most used first); ties keep the default order. Any previously-saved id
     * that is no longer in [ClaudeModels] is kept so the user can still see it.
     */
    fun modelsByUsage(): List<String> {
        val ids = ClaudeModels.IDS.toMutableList()
        if (model !in ids) ids.add(0, model)
        return ids.sortedByDescending { sendCount(it) }
    }

    private fun tokensKey(model: String) = "tokens_$model"
    private fun countKey(model: String) = "count_$model"

    // ---- account token rate limit (from response headers) ----

    fun saveRateLimit(limit: Long, remaining: Long, resetIso: String) = prefs.edit {
        putLong(KEY_RL_LIMIT, limit)
        putLong(KEY_RL_REMAINING, remaining)
        putString(KEY_RL_RESET, resetIso)
    }

    fun rateLimit(): RateLimit? {
        val limit = prefs.getLong(KEY_RL_LIMIT, -1L)
        if (limit <= 0L) return null
        return RateLimit(
            limit = limit,
            remaining = prefs.getLong(KEY_RL_REMAINING, 0L),
            resetIso = prefs.getString(KEY_RL_RESET, "").orEmpty(),
        )
    }

    companion object {
        const val DEFAULT_MODEL = "claude-opus-5"
        const val SCREEN_TEXT = "text"
        const val SCREEN_VOICE = "voice"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_DEFAULT_SCREEN = "default_screen"
        private const val KEY_VOICE_TONE = "voice_tone"
        private const val KEY_RL_LIMIT = "rl_tokens_limit"
        private const val KEY_RL_REMAINING = "rl_tokens_remaining"
        private const val KEY_RL_RESET = "rl_tokens_reset"
    }
}
