# Architecture

## Why a standalone app, not an in-launcher service

forgecity-launcher (Foundry) already has a notification-listener + Gemini pipeline
baked into the launcher APK (`buzz.delena.forgecity.assistant`). Crier is a deliberate
split-out: separate application id, separate notification-access grant, separate
release cadence. Rationale (from the design discussion that preceded this repo):

- The listener's reliability shouldn't depend on the launcher's UI process staying
  healthy — Foundry's own release notes carry a long history of Filament/Compose UI
  bugs on the primary test device.
- A standalone app is independently reusable regardless of which launcher (or none)
  the phone is running.
- Config + notification history become a real product surface instead of a settings
  sheet buried in launcher code.

Trade-off accepted: a second notification-access permission grant, a second app to
build/sign/release. No shared `:core` module exists yet — the notify/gemini/settings
code here is a fresh implementation following the same proven patterns (dedupe,
speech filter, quiet hours, Keystore-encrypted API key) as forgecity-launcher's
`assistant/` package, not a copy-paste of it.

## Pipeline (v0.1.6)

```
StatusBarNotification
  → CrierNotificationListenerService (dedupe, quiet-hours, privacy gate, allowlist filter, ongoing/FGS filters)
  → CrierPipelineBus (in-process unlimited Channel)
  → CrierForegroundService
      ├─ CallStateGate.isCallActive == true  → SpeechQueue.enqueue
      └─ CallStateGate.isCallActive == false → GeminiTtsClient.synthesize → GeminiAudioPlayer.play
  → CallStateGate flips to idle → SpeechQueue.drainReady() → speak each queued line
```

- `CrierNotificationListenerService` and `CrierForegroundService` run in the app's
  default process (not process-isolated). The foreground service keeps that process
  at foreground priority so the OS is much less likely to kill it.
- `CrierLogBus` (ring-buffer StateFlow) streams live debug, error, and API diagnostic events (URL, payload JSON, response JSON) to the in-app `LogsScreen`.
- `CrierPipelineBus` (Channel) and `CrierStatusBus` (StateFlow) are in-process
  singletons.
- Never stores notification title/body on disk — privacy-first default. Opt-in encrypted history storage is a v0.2.0 item.

## Gemini Audio & Model Compatibility

- **TTS Engine**: `GeminiTtsClient` calls Google Gemini `v1beta` REST endpoint `generateContent` with `responseModalities: ["AUDIO"]` and `voiceConfig: { prebuiltVoiceConfig: { voiceName: ... } }`.
- **Instruction Compatibility & Auto-Recovery**: For dedicated audio preview models (`gemini-3.1-flash-tts-preview`, `gemini-2.5-flash-preview-tts`, `gemini-2.5-pro-preview-tts`) where `systemInstruction` is restricted by the Gemini backend (`HTTP 400 Developer instruction is not enabled`), the client automatically inlines system persona instructions into the prompt text and features auto-retry fallback.
- **Audio Output**: `GeminiAudioPlayer` decodes both WAV container headers (RIFF/fmt/data) and raw PCM (16-bit linear PCM, mono/stereo, 8kHz–48kHz), streaming audio via `AudioTrack` routed to `USAGE_MEDIA` on `STREAM_MUSIC`.

## Call-aware queueing

`CallStateGate` wraps `TelephonyManager` (`TelephonyCallback` on API 31+,
`PhoneStateListener` below that — `minSdk` is 26). `RINGING` and `OFFHOOK` both count
as "wait" so a notification doesn't talk over an incoming call's ring.
`SpeechQueue` is a thread-safe bounded FIFO (max 20 items / 10 minute max age)
drained automatically when calls end.

## Config + secrets

`CrierSettingsStore` — SharedPreferences file `crier_settings`, Gemini API key
AES-GCM encrypted with an Android Keystore-backed 256-bit key (alias
`crier_gemini_api_key`). The key never leaves the Keystore; only the
ciphertext + IV live in prefs.
