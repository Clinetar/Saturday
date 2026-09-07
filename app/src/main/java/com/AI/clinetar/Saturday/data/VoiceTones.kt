package com.AI.clinetar.Saturday.data

/** A text-to-speech "tone": a pitch + speech-rate preset for the voice screen. */
data class VoiceTone(
    val id: String,
    val label: String,
    val description: String,
    val pitch: Float,
    val rate: Float,
)

object VoiceTones {

    val ALL: List<VoiceTone> = listOf(
        VoiceTone("natural", "Natural", "Default pitch and pace", pitch = 1.0f, rate = 1.0f),
        VoiceTone("warm", "Warm", "Lower and a touch slower", pitch = 0.9f, rate = 0.95f),
        VoiceTone("deep", "Deep", "Low and steady", pitch = 0.78f, rate = 0.95f),
        VoiceTone("bright", "Bright", "Higher and lively", pitch = 1.18f, rate = 1.05f),
        VoiceTone("calm", "Calm", "Gentle and unhurried", pitch = 0.96f, rate = 0.82f),
        VoiceTone("brisk", "Brisk", "Normal pitch, fast pace", pitch = 1.02f, rate = 1.3f),
    )

    const val DEFAULT_ID = "natural"

    fun find(id: String): VoiceTone = ALL.firstOrNull { it.id == id } ?: ALL.first()
}
