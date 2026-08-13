# Roadmap

## v0.1.0 (this release)

Notification listener + filter/dedupe/quiet-hours, call-aware speech queue,
persistent foreground relay, Keystore-encrypted Gemini API key, live TTS playground
(model/voice/language), About screen with name+version.

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
