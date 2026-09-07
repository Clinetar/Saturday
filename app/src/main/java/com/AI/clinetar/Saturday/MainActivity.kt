package com.AI.clinetar.Saturday

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.AI.clinetar.Saturday.data.SettingsStore
import com.AI.clinetar.Saturday.ui.BackendSettingsScreen
import com.AI.clinetar.Saturday.ui.ChatScreen
import com.AI.clinetar.Saturday.ui.SettingsScreen
import com.AI.clinetar.Saturday.ui.VoiceScreen
import com.AI.clinetar.Saturday.ui.VoiceSettingsScreen
import com.AI.clinetar.Saturday.ui.theme.SaturdayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SaturdayTheme {
                SaturdayApp()
            }
        }
    }
}

private enum class Route { CHAT, VOICE, SETTINGS, SETTINGS_BACKEND, SETTINGS_VOICE }

@Composable
private fun SaturdayApp() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel: ChatViewModel = viewModel()
    val store = remember { SettingsStore(context) }

    // Chat is always the root; if the default screen is Voice we open one level in.
    val backStack = rememberSaveable(
        saver = listSaver(
            save = { stack -> stack.map { it.name } },
            restore = { names -> names.map { Route.valueOf(it) }.toMutableStateList() },
        ),
    ) {
        val start = if (store.defaultScreen == SettingsStore.SCREEN_VOICE) {
            listOf(Route.CHAT, Route.VOICE)
        } else {
            listOf(Route.CHAT)
        }
        start.toMutableStateList()
    }

    val current = backStack.last()

    fun navigate(route: Route) {
        if (backStack.last() != route) backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    LaunchedEffect(current) {
        if (current == Route.CHAT) viewModel.refreshSettings()
    }

    // Back button / swipe: from any screen except the root, go one page back.
    BackHandler(enabled = backStack.size > 1) { pop() }

    // On the root (chat) screen, require two presses within 2s to leave the app.
    var lastBackAt by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = backStack.size == 1) {
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 2_000L) {
            activity?.finish()
        } else {
            lastBackAt = now
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    when (current) {
        Route.CHAT -> ChatScreen(
            viewModel = viewModel,
            onOpenSettings = { navigate(Route.SETTINGS) },
            onOpenVoice = { navigate(Route.VOICE) },
        )

        Route.VOICE -> VoiceScreen(
            viewModel = viewModel,
            store = store,
            onBack = { pop() },
        )

        Route.SETTINGS -> SettingsScreen(
            store = store,
            onBack = { pop() },
            onOpenBackend = { navigate(Route.SETTINGS_BACKEND) },
            onOpenVoice = { navigate(Route.SETTINGS_VOICE) },
        )

        Route.SETTINGS_BACKEND -> BackendSettingsScreen(
            store = store,
            onBack = { pop() },
        )

        Route.SETTINGS_VOICE -> VoiceSettingsScreen(
            store = store,
            onBack = { pop() },
        )
    }
}
