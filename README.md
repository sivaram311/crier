# Crier

**Crier** reads your Android notifications aloud with Gemini voices — quietly, in the
background, waiting out phone calls instead of talking over them. It's a standalone
notification-relay app: no launcher attached, own notification-access grant, own
config, own release cadence.

Package `buzz.delena.crier`. Sandbox DEV project (see [`docs/OPS.md`](docs/OPS.md)) —
no host ports, no Postgres, no CSS for this APK.

**Latest:** `v0.1.4-town-crier-dev` · versionCode **5**

## What it does (v0.1.4)

- Notification-listener service filters (per-app allowlist or allow-all, lock-screen speech, quiet hours, dedupe) and
  speaks the notification via Gemini's native-audio TTS (`generateContent`,
  `responseModalities: ["AUDIO"]`).
- Plays audio through Android's media stream with full WAV container and raw PCM decoding support in `GeminiAudioPlayer`.
- Seamless in-app navigation with Back buttons across Settings, Playground, and About screens.
- Runs as a persistent low-priority foreground service so the relay survives after
  you close the app — see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for exactly
  what that does and doesn't guarantee against OEM battery managers.
- **Call-aware queueing**: notifications arriving while a call is ringing or active
  are queued, not spoken; the queue drains automatically once the call ends.
- **Playground** screen: browse every Gemini voice model this build knows about
  (TTS live-wired; STT and Live API cataloged for v0.2.0) and test-speak with any
  model/voice/language combination.
- Gemini API key is AES-GCM encrypted with an Android Keystore-backed key — same
  pattern proven in forgecity-launcher's `AssistantSettingsStore`.
- About screen shows app name + version (`buzz/delena/crier/ui/about/AboutScreen.kt`).

## What's explicitly not in v0.1.4

See [`docs/ROADMAP.md`](docs/ROADMAP.md) — Gemini Live API (real-time bidirectional
voice), STT, the full per-parameter playground (rate/pitch/turn config), and
opt-in encrypted notification history storage are staged for v0.2.0+, not stubbed
here.

## Build

```powershell
.\gradlew.bat assembleDebug
```

## Install

Sideload the debug APK from a tagged GitHub release:

```powershell
curl.exe -L -o crier-0.1.4-town-crier-dev-debug.apk `
  https://github.com/sivaram311/crier/releases/download/v0.1.4-town-crier-dev/crier-0.1.4-town-crier-dev-debug.apk
Get-FileHash .\crier-0.1.4-town-crier-dev-debug.apk -Algorithm SHA256
# expect 558CA6557E6F5C89D7A11BD553F89C8817C4DED0A64443CCFB8CA1877DAC6E5F
adb install crier-0.1.4-town-crier-dev-debug.apk
```

Then grant notification access (Home screen has a shortcut button), add a Gemini
API key in Settings, and exempt the app from battery optimization if you want the
relay to survive aggressive OEM background kill (Realme included — see
`docs/VERIFICATION.md`).

## Docs

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — pipeline, process model, why a
  standalone app instead of an in-launcher service
- [`docs/OPS.md`](docs/OPS.md) — sandbox DEV scope, no ports/DB/CSS
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — v0.2.0+ plan
- [`docs/VERIFICATION.md`](docs/VERIFICATION.md) — device-confirmation status
- [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md) — dependency versions/tags
- [`docs/aidlc/INCEPTION-BASELINE.md`](docs/aidlc/INCEPTION-BASELINE.md) — AI-DLC
  as-is baseline
