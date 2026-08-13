# Crier

**Crier** reads your Android notifications aloud with Gemini voices — quietly, in the
background, waiting out phone calls instead of talking over them. It's a standalone
notification-relay app: no launcher attached, own notification-access grant, own
config, own release cadence.

Package `buzz.delena.crier`. Sandbox DEV project (see [`docs/OPS.md`](docs/OPS.md)) —
no host ports, no Postgres, no CSS for this APK.

**Latest:** `v0.1.6-town-crier-dev` · versionCode **7**

## What it does (v0.1.6)

- **Universal Model Compatibility**: Supports dedicated TTS preview models (`gemini-3.1-flash-tts-preview`, `gemini-2.5-flash-preview-tts`, `gemini-2.5-pro-preview-tts`) as well as all Gemini 2.0/2.5/3.x models with automated instruction-inlining and developer-instruction error recovery.
- **Configurable Notification Filters**: Fine-grained switches for ongoing sticky notifications, foreground service status alerts, and quiet hours in Settings.
- **Live Logs**: Real-time debug log viewer with full request payloads and API responses, tag filtering, copy, and clear functionality.
- **Dynamic Model Discovery**: Queries Gemini API (`GET /v1beta/models`) with your API key to list and pick from all available Gemini models.
- **System Prompt / Persona**: Customizable system instructions to direct Gemini on how to phrase, summarize, and synthesize spoken notifications.
- **Full Notification Text Context**: Transmits complete rich notification content without early truncation.
- **Read-only Saved Key**: Shows complete active key in read-only mode after saving, with full edit/clear controls.
- **Lock-screen speech**: Configurable `speakWhenLocked` setting for reading notifications aloud with screen locked/in pocket.
- **Streaming Audio Decoder**: Full WAV container parsing, stereo/mono support, and streaming `AudioTrack` output.
- **Call-aware queueing**: notifications arriving while a call is ringing or active
  are queued, not spoken; the queue drains automatically once the call ends.
- **Playground**: Test-speak with live models, voices, languages, and custom system prompts with immediate Live Log feedback.
- Gemini API key is AES-GCM encrypted with an Android Keystore-backed key.

## What's explicitly not in v0.1.6

See [`docs/ROADMAP.md`](docs/ROADMAP.md) — Gemini Live API (real-time bidirectional
voice), STT, and opt-in encrypted notification history storage are staged for v0.2.0+.

## Build

```powershell
.\gradlew.bat assembleDebug
```

## Install

Sideload the debug APK from a tagged GitHub release:

```powershell
curl.exe -L -o crier-0.1.6-town-crier-dev-debug.apk `
  https://github.com/sivaram311/crier/releases/download/v0.1.6-town-crier-dev/crier-0.1.6-town-crier-dev-debug.apk
Get-FileHash .\crier-0.1.6-town-crier-dev-debug.apk -Algorithm SHA256
# expect 6830656F3EDEA4F0C964593C7F190517564B4F9DEC485B5331EE8C3F16615226
adb install crier-0.1.6-town-crier-dev-debug.apk
```

## Docs

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — pipeline, process model, why a
  standalone app instead of an in-launcher service
- [`docs/OPS.md`](docs/OPS.md) — sandbox DEV scope, no ports/DB/CSS
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — v0.2.0+ plan
- [`docs/VERIFICATION.md`](docs/VERIFICATION.md) — device-confirmation status
- [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md) — dependency versions/tags
- [`docs/aidlc/INCEPTION-BASELINE.md`](docs/aidlc/INCEPTION-BASELINE.md) — AI-DLC
  as-is baseline
