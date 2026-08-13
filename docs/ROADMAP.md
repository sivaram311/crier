# Roadmap

## v0.1.0 (this release)

Notification listener + filter/dedupe/quiet-hours, call-aware speech queue,
persistent foreground relay, Keystore-encrypted Gemini API key, live TTS playground
(model/voice/language), About screen with name+version.

## v0.1.1 (near-term polish, from two independent code review rounds)

Not blocking the v0.1.0 ship (the High-severity findings from both rounds — dead
`READ_PHONE_STATE`, unsynchronized `SpeechQueue`, dropping `SharedFlow`, no
lock-screen gate, pipeline death on exception, unjustified `RECORD_AUDIO` — were
fixed before shipping). Remaining flagged gaps, deferred and documented rather than
silently dropped:

- Quiet-hours on/off switch + time pickers in Settings (currently fixed 22:00–07:00,
  no UI, no way to disable).
- Surface `GeminiTtsClient` failures (`Unauthorized`/`Timeout`/`Malformed`) on Home
  instead of silently doing nothing — `CrierStatus.lastError`.
- Home should explicitly say "pick apps in Settings" when the allowlist is empty
  (currently just silently speaks nothing).
- Edge-to-edge system-bar/IME padding on the root surface.
- `CrierNotificationListenerService.onListenerConnected` starts the foreground
  service even when `assistantEnabled` is false, showing a persistent "Crier is
  listening" notification with nothing to relay.
- A release-signed (`debuggable=false`) build for anyone who wants a Gemini key
  harder to pull via `adb`/`run-as` than the current `assembleDebug` sideload —
  same debug-APK distribution model as forgecity-launcher today, so treated as an
  accepted tradeoff for this sandbox release, not a blocker.

## v0.2.0 (planned)

- **Gemini Live API** wiring (`gemini-2.5-flash-native-audio-preview-09-2025` /
  `gemini-live-2.5-flash-preview`) — real-time bidirectional voice conversation from
  the Playground, not just one-shot TTS.
- **STT** wiring for the cataloged transcription model.
- **Full parameter playground**: speaking rate/pitch, turn/VAD sensitivity for Live
  API, system-prompt/persona field, sample rate — every knob Gemini exposes, not
  just model/voice/language.
- **Live model catalog fetch** (`GET /v1beta/models`) instead of the static seed list
  in `GeminiModelCatalog`, so new Gemini models show up without an app update.
- **Opt-in, per-package notification history storage** — encrypted Room DB, separate
  from any future launcher DB, retention cap enforced by WorkManager. Off by default,
  reversing nothing about the current "never store title/body" default without an
  explicit opt-in.
- **Process isolation** (`android:process=":relay"` for the listener + foreground
  service) plus a real cross-process bridge (Messenger/AIDL or a signature-permission
  ContentProvider) replacing the in-process `CrierPipelineBus`/`CrierStatusBus`
  SharedFlow/StateFlow singletons — considered in v0.1.0's architecture doc, deferred
  to keep the first ship scoped.
- Realme-specific OEM autostart-allowlist onboarding copy (can't be requested via
  intent; needs a manual Settings walkthrough in-app).

## Explicit non-goals for now

- No companion web console / dashboard.
- No multi-user or cloud sync — on-device only.
