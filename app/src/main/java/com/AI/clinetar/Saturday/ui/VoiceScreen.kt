package com.AI.clinetar.Saturday.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.AI.clinetar.Saturday.Author
import com.AI.clinetar.Saturday.ChatViewModel
import com.AI.clinetar.Saturday.data.SettingsStore
import com.AI.clinetar.Saturday.data.VoiceTones
import java.util.Locale

enum class VoicePhase { IDLE, LISTENING, THINKING, SPEAKING, ERROR, UNAVAILABLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    viewModel: ChatViewModel,
    store: SettingsStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val engine = remember { VoiceEngine(context, store) { transcript -> viewModel.send(transcript) } }
    DisposableEffect(Unit) {
        engine.lastHandledId = viewModel.state.value.messages.lastOrNull()?.id ?: -1L
        onDispose { engine.release() }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) engine.startConversation()
    }

    // Speak a reply once it lands, but only when this screen triggered the turn.
    LaunchedEffect(state.messages, state.isSending) {
        if (engine.phase == VoicePhase.THINKING && !state.isSending) {
            val last = state.messages.lastOrNull()
            if (last != null && last.author == Author.CLAUDE && last.id != engine.lastHandledId) {
                engine.lastHandledId = last.id
                engine.speak(last.text)
            }
        }
    }

    val lastUser = state.messages.lastOrNull { it.author == Author.USER && !it.isError }?.text
    val lastReply = state.messages.lastOrNull { it.author == Author.CLAUDE }?.text

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = {
                            engine.stop()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text("Talk") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                if (lastUser == null && lastReply == null) {
                    Text(
                        "Tap the mic and start talking.\nClaude replies out loud, then listens again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    if (lastUser != null) {
                        Text(
                            "You",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(lastUser, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(20.dp))
                    }
                    if (lastReply != null) {
                        Text(
                            "Claude",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            lastReply,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Text(
                statusText(engine.phase, hasPermission),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            engine.errorText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(20.dp))

            MicButton(
                phase = engine.phase,
                level = engine.level,
                enabled = engine.phase != VoicePhase.UNAVAILABLE,
                onClick = {
                    if (!hasPermission) {
                        permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        engine.toggle()
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun statusText(phase: VoicePhase, hasPermission: Boolean): String = when {
    phase == VoicePhase.UNAVAILABLE -> "No speech recognition available on this device."
    !hasPermission -> "Microphone access needed — tap the mic to grant it."
    phase == VoicePhase.LISTENING -> "Listening…"
    phase == VoicePhase.THINKING -> "Thinking…"
    phase == VoicePhase.SPEAKING -> "Speaking…"
    phase == VoicePhase.ERROR -> "Tap to try again"
    else -> "Tap to start talking"
}

@Composable
private fun MicButton(
    phase: VoicePhase,
    level: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "mic")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulse",
    )
    val scale = when (phase) {
        VoicePhase.LISTENING -> 1f + level * 0.35f
        VoicePhase.THINKING, VoicePhase.SPEAKING -> pulse
        else -> 1f
    }
    val target = when (phase) {
        VoicePhase.LISTENING -> MaterialTheme.colorScheme.primary
        VoicePhase.SPEAKING -> MaterialTheme.colorScheme.tertiary
        VoicePhase.THINKING -> MaterialTheme.colorScheme.secondary
        VoicePhase.ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when (phase) {
        VoicePhase.LISTENING -> MaterialTheme.colorScheme.onPrimary
        VoicePhase.SPEAKING -> MaterialTheme.colorScheme.onTertiary
        VoicePhase.THINKING -> MaterialTheme.colorScheme.onSecondary
        VoicePhase.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val container by animateColorAsState(target, label = "container")

    val active = phase == VoicePhase.LISTENING ||
        phase == VoicePhase.THINKING ||
        phase == VoicePhase.SPEAKING

    Box(
        modifier = Modifier
            .size(128.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(container)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (active) "Stop" else "Start talking",
            tint = content,
            modifier = Modifier.size(48.dp),
        )
    }
}

/**
 * Owns the platform speech recogniser + text-to-speech and drives a hands-free
 * loop: listen → hand the transcript to the caller → (caller speaks the reply) →
 * listen again, until [stop].
 */
private class VoiceEngine(
    context: Context,
    private val settings: SettingsStore,
    private val onTranscript: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val available = SpeechRecognizer.isRecognitionAvailable(appContext)

    var phase by mutableStateOf(if (available) VoicePhase.IDLE else VoicePhase.UNAVAILABLE)
        private set
    var level by mutableFloatStateOf(0f)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    /** Id of the last CLAUDE message we've already spoken (or seen before entering). */
    var lastHandledId: Long = -1L

    private var conversationActive = false
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeak: String? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.getDefault()
                applyTone()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        main.post { afterSpeaking() }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        main.post { afterSpeaking() }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        main.post { afterSpeaking() }
                    }
                })
                main.post {
                    pendingSpeak?.let { speak(it) }
                    pendingSpeak = null
                }
            }
        }
    }

    fun toggle() {
        if (phase == VoicePhase.UNAVAILABLE) return
        if (conversationActive) stop() else startConversation()
    }

    fun startConversation() {
        if (!available) return
        errorText = null
        conversationActive = true
        startListening()
    }

    fun stop() {
        conversationActive = false
        recognizer?.cancel()
        tts?.stop()
        level = 0f
        if (phase != VoicePhase.UNAVAILABLE) phase = VoicePhase.IDLE
    }

    fun speak(text: String) {
        recognizer?.cancel()
        val engine = tts
        if (engine == null || !ttsReady) {
            pendingSpeak = text
            phase = VoicePhase.SPEAKING
            return
        }
        applyTone()
        phase = VoicePhase.SPEAKING
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    private fun applyTone() {
        val tone = VoiceTones.find(settings.voiceTone)
        tts?.setPitch(tone.pitch)
        tts?.setSpeechRate(tone.rate)
    }

    fun release() {
        conversationActive = false
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun afterSpeaking() {
        if (conversationActive) startListening() else phase = VoicePhase.IDLE
    }

    private fun startListening() {
        if (!available || !conversationActive) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        level = 0f
        phase = VoicePhase.LISTENING
        recognizer?.startListening(intent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onRmsChanged(rmsdB: Float) {
            if (phase == VoicePhase.LISTENING) level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onEndOfSpeech() {
            level = 0f
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            when {
                text.isNotEmpty() -> {
                    phase = VoicePhase.THINKING
                    onTranscript(text)
                }
                conversationActive -> startListening()
                else -> phase = VoicePhase.IDLE
            }
        }

        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> if (conversationActive) startListening() else phase = VoicePhase.IDLE

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    if (conversationActive) main.postDelayed({ startListening() }, 350L)

                else -> {
                    conversationActive = false
                    level = 0f
                    phase = VoicePhase.ERROR
                    errorText = errorMessage(error)
                }
            }
        }
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Network error reaching the speech service."
        SpeechRecognizer.ERROR_AUDIO -> "Couldn't record audio."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recogniser error — try again."
        else -> "Speech recognition failed (code $code)."
    }
}
