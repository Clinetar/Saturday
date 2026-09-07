package com.AI.clinetar.Saturday.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.AI.clinetar.Saturday.data.SettingsStore
import com.AI.clinetar.Saturday.data.VoiceTone
import com.AI.clinetar.Saturday.data.VoiceTones
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(store.voiceTone) }
    var ready by remember { mutableStateOf(false) }

    val ttsHolder = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.getDefault()
                ready = true
            }
        }
        ttsHolder.value = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
            ttsHolder.value = null
        }
    }

    fun preview(tone: VoiceTone) {
        val engine = ttsHolder.value ?: return
        engine.setPitch(tone.pitch)
        engine.setSpeechRate(tone.rate)
        engine.speak(
            "Hi, this is how I'll sound.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "preview",
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Tone used when Claude speaks on the talk screen. Tap one to hear it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
            HorizontalDivider()

            VoiceTones.ALL.forEach { tone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedId = tone.id
                            store.voiceTone = tone.id
                            preview(tone)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedId == tone.id,
                        onClick = {
                            selectedId = tone.id
                            store.voiceTone = tone.id
                            preview(tone)
                        },
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(tone.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            tone.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { preview(tone) },
                        enabled = ready,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play sample")
                    }
                }
                HorizontalDivider()
            }

            if (!ready) {
                Text(
                    "Loading text-to-speech…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}
