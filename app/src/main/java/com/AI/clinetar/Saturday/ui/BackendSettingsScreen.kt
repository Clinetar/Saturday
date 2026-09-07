package com.AI.clinetar.Saturday.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.AI.clinetar.Saturday.data.ClaudeModels
import com.AI.clinetar.Saturday.data.RateLimit
import com.AI.clinetar.Saturday.data.SettingsStore
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendSettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit,
) {
    // The real key is only ever held in state while the user is typing a new one.
    var newKey by remember { mutableStateOf("") }
    var editingKey by remember { mutableStateOf(!store.hasApiKey) }
    var model by remember { mutableStateOf(store.model) }
    var revealKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var keyHint by remember { mutableStateOf<String?>(null) }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val orderedModels = remember { store.modelsByUsage() }
    val mostUsedId = remember { orderedModels.firstOrNull { store.sendCount(it) > 0 } }
    val tokens = remember(model, saved) { store.tokensUsed(model) }
    val rateLimit = remember(saved) { store.rateLimit() }
    val selected = ClaudeModels.find(model)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backend AI") },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text("Anthropic API key", style = MaterialTheme.typography.titleMedium)

            if (!editingKey && store.hasApiKey) {
                // Locked: only the last 4 chars, as plain (non-selectable) text.
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        store.apiKeyMasked,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        editingKey = true
                        newKey = ""
                        revealKey = false
                        keyHint = null
                        saved = false
                    }) { Text("Replace") }
                }
                Text(
                    "Hidden after saving — only the last 4 characters are shown, and it can't be copied back out. Stored on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it; saved = false },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    placeholder = { Text("sk-ant-...") },
                    visualTransformation = if (revealKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                    trailingIcon = {
                        IconButton(onClick = { revealKey = !revealKey }) {
                            Icon(
                                if (revealKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (revealKey) "Hide key" else "Show key",
                            )
                        }
                    },
                )
                Text(
                    "Create a key in the Console, copy it, then come back and paste. " +
                        "Once you save, it's hidden for good — only the last 4 stay visible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            keyHint = null
                            runCatching { uriHandler.openUri(CONSOLE_KEYS_URL) }
                                .onFailure { keyHint = "Couldn't open a browser on this device." }
                        },
                    ) { Text("Get a key") }
                    TextButton(
                        onClick = {
                            val fromClip = clipboardApiKey(context)
                            if (fromClip != null) {
                                newKey = fromClip
                                saved = false
                                keyHint = "Pasted key from clipboard — tap Save."
                            } else {
                                keyHint = "No API key on the clipboard. Copy it from the Console first."
                            }
                        },
                    ) { Text("Paste from clipboard") }
                    if (store.hasApiKey) {
                        TextButton(onClick = {
                            editingKey = false
                            newKey = ""
                            revealKey = false
                            keyHint = null
                        }) { Text("Cancel") }
                    }
                }
                keyHint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Text(
                "Model",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = ClaudeModels.labelFor(model),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    supportingText = { Text(model) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    orderedModels.forEach { id ->
                        val m = ClaudeModels.find(id)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(ClaudeModels.labelFor(id))
                                        if (id == mostUsedId) {
                                            Text(
                                                "  · most used",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    if (m != null) {
                                        SegmentBar(
                                            active = m.costBars,
                                            barHeight = 5.dp,
                                            modifier = Modifier.width(120.dp).padding(top = 5.dp),
                                        )
                                    }
                                }
                            },
                            onClick = {
                                model = id
                                expanded = false
                                saved = false
                            },
                            leadingIcon = if (id == model) {
                                { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                            } else null,
                        )
                    }
                }
            }

            if (selected != null) {
                Text(
                    "Cost per run",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                SegmentBar(
                    active = selected.costBars,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    barHeight = 18.dp,
                )
                Text(
                    "$%,.0f in / $%,.0f out per 1M tokens — light (green) to expensive (red)."
                        .format(selected.inputPer1M, selected.outputPer1M),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Text(
                "Your usage of ${ClaudeModels.labelFor(model)}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp),
            )
            SegmentBar(
                active = barsForTokens(tokens),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                barHeight = 26.dp,
            )
            Text(
                "%,d tokens used on this device with this model.".format(tokens),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(top = 24.dp))
            Text(
                "Account token budget",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            AccountBudget(rateLimit)

            Button(
                onClick = {
                    if (editingKey && newKey.isNotBlank()) {
                        store.apiKey = newKey
                    }
                    store.model = model
                    model = store.model
                    if (store.hasApiKey) {
                        editingKey = false
                        newKey = ""
                        revealKey = false
                    }
                    saved = true
                },
                modifier = Modifier.padding(top = 28.dp),
            ) {
                Text("Save")
            }
            if (saved) {
                Text(
                    "Saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountBudget(rl: RateLimit?) {
    if (rl == null) {
        Text(
            "Send a message to load your key's per-minute token limit. " +
                "(Anthropic doesn't expose a true credit balance to API keys — this is the " +
                "shared rate-limit budget, the closest account-wide number available.)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        return
    }
    val activeBars = ceil(rl.usedFraction * 5f).toInt().coerceIn(0, 5)
    SegmentBar(
        active = activeBars,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        barHeight = 26.dp,
    )
    Text(
        "%,d used  ·  %,d left  of  %,d tokens / min".format(rl.used, rl.remaining, rl.limit),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp),
    )
    resetHint(rl.resetIso)?.let {
        Text(
            "Refills $it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        "Anthropic's per-minute token rate limit for this key — the closest thing the " +
            "Messages API exposes to account-wide usage / remaining.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Five fixed green-to-red segments; the first [active] are filled. */
@Composable
private fun SegmentBar(
    active: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 26.dp,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BAR_COLORS.forEachIndexed { i, color ->
            val on = i < active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (on) color else color.copy(alpha = 0.14f))
                    .border(1.dp, color.copy(alpha = if (on) 0f else 0.45f), RoundedCornerShape(6.dp)),
            )
        }
    }
}

private const val CONSOLE_KEYS_URL = "https://console.anthropic.com/settings/keys"

/** Returns the clipboard text if it looks like an Anthropic API key, else null. */
private fun clipboardApiKey(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val text = cm.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        ?.trim()
    return text?.takeIf { it.startsWith("sk-ant-") && it.length in 20..400 && !it.any { c -> c.isWhitespace() } }
}

private val BAR_COLORS = listOf(
    Color(0xFF2E7D32), // green
    Color(0xFF9CCC65), // light green
    Color(0xFFF9A825), // amber
    Color(0xFFF57C00), // orange
    Color(0xFFC62828), // red
)

private fun barsForTokens(tokens: Long): Int = when {
    tokens <= 0L -> 0
    tokens < 25_000L -> 1
    tokens < 100_000L -> 2
    tokens < 400_000L -> 3
    tokens < 1_500_000L -> 4
    else -> 5
}

private fun resetHint(iso: String): String? {
    if (iso.isBlank()) return null
    return try {
        val secs = Duration.between(Instant.now(), Instant.parse(iso)).seconds
        when {
            secs <= 0L -> "any moment"
            secs < 60L -> "in ${secs}s"
            else -> "in ${secs / 60}m ${secs % 60}s"
        }
    } catch (_: Exception) {
        null
    }
}
