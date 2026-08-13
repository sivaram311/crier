# Roadmap

## v0.1.0 (this release)

Notification listener + filter/dedupe/quiet-hours, call-aware speech queue,
persistent foreground relay, Keystore-encrypted Gemini API key, live TTS playground
(model/voice/language), About screen with name+version.

## v0.1.1 (Released - near-term polish)

Implemented all identified gaps from review rounds:
- [x] Quiet-hours on/off switch + time pickers in Settings.
- [x] Surface `GeminiTtsClient` failures (`Unauthorized`/`Timeout`/`Malformed`) on Home — `CrierStatus.lastError`.
- [x] Home explicitly says "No apps allowed to speak" when the allowlist is empty.
- [x] Edge-to-edge system-bar/IME padding on the root surface.
- [x] `CrierNotificationListenerService.onListenerConnected` only starts the foreground service when `assistantEnabled` is true.
- [x] A release-signed (`debuggable=false`) build setup.

## v0.1.2 (Released - model catalog and synchronization polish)

- [x] Expanded `GeminiModelCatalog.kt` to include standard production and preview models: `gemini-2.0-flash`, `gemini-2.0-flash-lite-preview-02-05` (Flash Lite), `gemini-2.5-flash`, and `gemini-2.5-pro`.
- [x] Changed the default TTS model to `gemini-2.0-flash` for native speed, reliability, and cost-effectiveness.
- [x] Synchronized the active model selected in Settings with the Playground screen and vice-versa, saving selection state directly back to the persistent store.

## v0.1.3 (Released - edge-to-edge layout padding fix)

- [x] Fixed edge-to-edge blank screen issue by moving `systemBarsPadding()` and `imePadding()` layout modifiers from the absolute root `Surface` in `MainActivity.kt` to the individual screen level (`HomeScreen`, `SettingsScreen`, `PlaygroundScreen`, and `AboutScreen`).

## v0.1.4 (Released - lock-screen relay, streaming audio decoder, navigation & allowlist UX)

- [x] Added `speakWhenLocked` setting enabling Crier to speak incoming notifications when screen is locked/in pocket.
- [x] Enhanced `GeminiAudioPlayer` with robust WAV container parsing, stereo/mono support, streaming `AudioTrack` mode, and routing to media stream.
- [x] Added `allowAllApps` toggle, Select All / Clear All quick actions, and real-time app search filter in Settings.
- [x] Added in-app Back button navigation across Settings, Playground, and About screens.
- [x] Added API Key missing banner with direct action to Settings on Home screen.

## v0.1.5 (Released - live logs & API inspection, dynamic models, system prompt, full notification text)

- [x] Added `CrierLogBus` and `LogsScreen` for real-time in-app debug logging with expandable API request payload JSON and response body viewers, search, and copy-all.
- [x] Read-only API key box in Settings showing the complete key after saving, with Edit/Clear toggles.
- [x] Dynamic model discovery via `GET /v1beta/models` populating all models accessible to the user's API key.
- [x] Added customizable System Prompt / Persona in settings and playground injected into Gemini API `systemInstruction`.
- [x] Passing full notification text context without early truncation.

## v0.2.0 (planned)

- **Gemini Live API** wiring (`gemini-2.5-flash-native-audio-preview-09-2025` /
  `gemini-live-2.5-flash-preview`) — real-time bidirectional voice conversation from
  the Playground, not just one-shot TTS.
- **STT** wiring for the cataloged transcription model.
- **Opt-in, per-package notification history storage** — encrypted Room DB, separate
  from any future launcher DB, retention cap enforced by WorkManager. Off by default.
- **Process isolation** (`android:process=":relay"` for the listener + foreground
  service) plus a real cross-process bridge (Messenger/AIDL or a signature-permission
  ContentProvider) replacing the in-process `CrierPipelineBus`/`CrierStatusBus`
  SharedFlow/StateFlow singletons.

## Explicit non-goals for now

- No companion web console / dashboard.
- No multi-user or cloud sync — on-device only.
