# Saturday

A minimal Android chatbot that talks to the [Anthropic Messages API](https://docs.claude.com/en/api/messages).
Jetpack Compose UI, one Activity, no backend.

## What it does

- A chat screen: type a message, get a reply from Claude.
- A **voice screen** (waveform icon in the top bar): tap the mic and talk — it
  transcribes your speech (Android `SpeechRecognizer`), sends it, speaks the reply
  (`TextToSpeech`), then listens again. Same conversation as the text screen.
- The whole conversation is kept in memory and re-sent on every turn (Claude is stateless).
- A Settings screen to enter your Anthropic API key and pick a model from a dropdown
  (ordered most-used first), with a 5-segment green→red meter showing cumulative
  token spend on the selected model.
- Errors (bad key, rate limit, no network, unknown model) are shown inline as a chat bubble.

## Project layout

| File | Purpose |
|------|---------|
| `data/ClaudeApi.kt` | Serializable request/response DTOs for `/v1/messages` (incl. `usage`) |
| `data/ClaudeModels.kt` | The list of selectable Claude models + display labels |
| `data/ClaudeRepository.kt` | OkHttp call, header setup, error mapping, usage recording |
| `data/SettingsStore.kt` | API key, model, per-model message count + token totals (`SharedPreferences`) |
| `ChatViewModel.kt` | Holds the message list + send/clear logic |
| `MainActivity.kt` | Back stack + routing for the Chat / Voice / Settings screens |
| `ui/ChatScreen.kt` | Message list, bubbles, animated typing dots, input bar |
| `ui/VoiceScreen.kt` | Hands-free voice loop — `SpeechRecognizer` + `TextToSpeech` |
| `ui/SettingsScreen.kt` | Settings root menu: default screen + submenu links |
| `ui/BackendSettingsScreen.kt` | "Backend AI" submenu — API key, model, usage meters |
| `ui/VoiceSettingsScreen.kt` | "Voice" submenu — TTS tone picker with previews |
| `ui/theme/Theme.kt` | Single fixed pure-black (AMOLED) theme, mint-green accent |

## Navigation

One back stack in `MainActivity` (Chat is always the root). The back button and
the predictive-back swipe pop one screen from anywhere; on the Chat root, two
presses within 2s exit ("Press back again to exit"). Settings is a menu →
**Backend AI** and **Voice** submenus, plus a **Default screen** (Text / Voice)
toggle that decides which screen the app opens on.

## Running it

1. Open the project in Android Studio (or build from the command line — the Gradle
   wrapper is committed).
2. Build & install:
   ```
   ./gradlew :app:installDebug
   ```
3. Launch the app, open **Settings** (gear icon). Tap **Get a key** to open the
   Anthropic Console in your browser, create/copy a key, come back and tap
   **Paste from clipboard** (or paste into the field), then **Save**.
4. Go back and start chatting.

There is no "sign in with Anthropic" — the Messages API authenticates with an
API key, and a Claude Pro/Max subscription can't be used for API calls. The key
is pay-as-you-go (no monthly fee; usually some trial credit) and personal chat
volumes cost cents.

Once saved, the key is masked to its last 4 characters (`•••• •••• •••• XXXX`),
shown as plain non-selectable text — no reveal, no copy. The plaintext only
exists in `SharedPreferences` and in the entry field while you're typing a new
one. Use **Replace** to swap it.

Requires the Android SDK; `local.properties` points at it via `sdk.dir` and is not
checked in.

## Configuration

- **Model** — pick from the Settings dropdown (default `claude-opus-5`), ordered
  most-used-first. Each entry shows a green→red **cost-per-run** bar (from the
  list prices in `data/ClaudeModels.kt`, `costBars`).
- **Meters** in Settings (all the same 5-segment green→red scale):
  - *Cost per run* — how expensive the selected model is.
  - *Your usage* — cumulative tokens spent on this device with that model
    (`barsForTokens` in `ui/SettingsScreen.kt`).
  - *Account token budget* — the per-minute token rate limit for your key, read
    from the `anthropic-ratelimit-tokens-*` response headers, showing used / left
    / total and when it refills. (The Messages API doesn't expose a real credit
    balance to API keys, so this rate-limit bucket is the closest account-wide
    number.)
- **Max tokens / system prompt** — constants in `ClaudeRepository.kt`
  (`MAX_TOKENS`, `SYSTEM_PROMPT`).

## Known limitations / next steps

- **The API key ships to the device.** `SharedPreferences` is app-private but stored
  in plain text, and calling Anthropic directly from a client exposes the key to
  anyone who can pull it off the device. A real app should proxy requests through a
  backend that holds the key. This build is fine for personal use / a demo.
- **No streaming.** Replies arrive all at once after the full response is generated.
  Opus 5 uses adaptive thinking by default, so long answers can take a few seconds;
  switch the model to `claude-haiku-4-5` or add streaming
  (`/v1/messages` with `"stream": true`, parse the SSE events) for a snappier feel.
- Conversation history is lost when the process is killed (it lives only in the
  ViewModel). Persisting it would mean a Room database or serializing to disk.
- No retry/backoff on 429 or 5xx beyond what OkHttp does by default.

## Tooling versions

Gradle 9.7.1 · AGP 9.4.0 (built-in Kotlin) · Kotlin 2.2.10 · Compose BOM 2026.08.00 · minSdk 26 · compileSdk 37

Needs a full JDK (17+) with `javac` on the build machine — a headless JRE isn't
enough. Android Studio's bundled JDK works; from the CLI, point `JAVA_HOME` at a
JDK before running `./gradlew`.
