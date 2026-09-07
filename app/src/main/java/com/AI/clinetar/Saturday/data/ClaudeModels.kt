package com.AI.clinetar.Saturday.data

/** A Claude model the user can pick in Settings. */
data class ClaudeModel(
    val id: String,
    val label: String,
    /** USD per 1M input tokens (list price). */
    val inputPer1M: Double,
    /** USD per 1M output tokens (list price). */
    val outputPer1M: Double,
) {
    /**
     * 1..5 "how much this model costs to run" rating, driven by output price
     * (the dominant cost for a chat app). 1 = light/cheap, 5 = heavy/expensive.
     */
    val costBars: Int
        get() = when {
            outputPer1M <= 5.0 -> 1
            outputPer1M <= 10.0 -> 2
            outputPer1M <= 20.0 -> 3
            outputPer1M <= 30.0 -> 4
            else -> 5
        }
}

object ClaudeModels {

    /** Offered in the dropdown, in default (fallback) order. Prices: docs.claude.com. */
    val ALL: List<ClaudeModel> = listOf(
        ClaudeModel("claude-opus-5", "Opus 5", inputPer1M = 5.0, outputPer1M = 25.0),
        ClaudeModel("claude-sonnet-5", "Sonnet 5", inputPer1M = 2.0, outputPer1M = 10.0),
        ClaudeModel("claude-haiku-4-5", "Haiku 4.5", inputPer1M = 1.0, outputPer1M = 5.0),
        ClaudeModel("claude-opus-4-8", "Opus 4.8", inputPer1M = 5.0, outputPer1M = 25.0),
        ClaudeModel("claude-fable-5-1", "Fable 5.1", inputPer1M = 10.0, outputPer1M = 50.0),
    )

    val IDS: List<String> = ALL.map { it.id }

    fun find(id: String): ClaudeModel? = ALL.firstOrNull { it.id == id }

    fun labelFor(id: String): String = find(id)?.label ?: id
}
