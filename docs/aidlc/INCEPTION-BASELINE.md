# AI-DLC Inception Baseline - crier

**Captured:** 2026-08-13 (as-is snapshot, at first ship — not a target design)

## Purpose

Crier is a standalone on-device Android **notification-listener + Gemini voice
relay** (package `buzz.delena.crier`). It speaks incoming notifications aloud via
Gemini's native-audio TTS, runs persistently in the background via a foreground
service, and queues speech during phone calls instead of talking over them. It was
split out of forgecity-launcher's in-app `assistant/` notification pipeline as a
separate app on the user's explicit direction, so its reliability and release cycle
don't depend on any launcher's UI process. Distribution is via public GitHub
prerelease debug APKs.

## Tech stack

| Layer | As stated in repo |
|-------|-------------------|
| Language / UI | Kotlin + Jetpack Compose (Material3, Navigation Compose); JVM target 17 |
| Build | Android Gradle Plugin 8.11.1; Kotlin 2.3.21; KSP 2.3.10; single module `:app` |
| SDK | `compileSdk` / `targetSdk` 35; `minSdk` 26 |
| Compose BOM | `androidx.compose:compose-bom:2025.07.00` |
| Persistence | Encrypted SharedPreferences (Keystore AES-GCM) for config; no notification-content storage in v0.1.0 |
| Background work | Foreground `Service` (`CrierForegroundService`) + `NotificationListenerService`; WorkManager dependency present, unused until v0.2.0 history-retention job |
| Network | Plain `HttpsURLConnection` client to Gemini `v1beta` REST — no SDK, no OkHttp |
| Tests | JUnit 4.13.2 unit tests on pure logic (dedupe, filter, quiet hours, speech queue) |
| App id / version | `buzz.delena.crier` · `versionName` `0.1.0-town-crier-dev` · `versionCode` 1 |

No `package.json`, `pom.xml`, or Docker/Compose files exist in this repo — same as
forgecity-launcher, this is a plain Android Gradle project.

## Current features (as-built, v0.1.0)

- **Notification listener** (`CrierNotificationListenerService`) — dedupe (in-memory,
  no content persisted), quiet-hours gate, per-package allowlist filter.
- **Call-aware speech queue** — `CallStateGate` (TelephonyManager) + `SpeechQueue`
  (pure, bounded FIFO); notifications during a call are queued and drained once the
  call ends.
- **Persistent background relay** — `CrierForegroundService`, low-priority ongoing
  notification, `START_STICKY`, boot-restart receiver.
- **Gemini TTS** — `GeminiTtsClient` (native-audio `generateContent`), played via
  `GeminiAudioPlayer` (`AudioTrack`, static PCM buffer).
- **Playground** — model/voice/language picker across the TTS/STT/Live catalog
  (`GeminiModelCatalog`); only TTS is wired to a live network call in this build.
- **Settings** — encrypted API key, model/voice/language, per-installed-app
  allowlist toggles.
- **About screen** — app name + version, satisfying CONSCIOUS rule 24 from first
  DEV deploy (this is a new build, not retrofitted).
- Battery-optimization exemption request flow (`BatteryOptimization`).

## Deploy topology

- Android APK, not a hosted web service. No web port reserved (see `docs/OPS.md` —
  "No host ports / Postgres / CSS for this APK", same waiver language as
  forgecity-launcher).
- No F:/G: deploy — mobile app distribution is GitHub releases, not the drive-based
  promote pipeline. Q1/Q2 evidence-pack gates were judged not applicable for the same
  reason forgecity-launcher's `docs/ARCHITECTURE.md` states: "DEV-only sandbox
  project. No F:/G: deploy, no nginx host, no CSS client."
- Outbound HTTPS only to `generativelanguage.googleapis.com` with a user-supplied key.

## Known debt / gaps (as-is, factual, first ship)

- **Not device-confirmed** — no physical Android device attached to the build host;
  build validated via `gradlew assembleDebug` + JVM unit tests only. See
  `docs/VERIFICATION.md`.
- **Single process** — listener + foreground service + UI share one process for
  v0.1.0; process isolation deferred (documented in `docs/ARCHITECTURE.md` /
  `docs/ROADMAP.md`), not an oversight.
- **Live API / STT catalog-only** — not wired to any network call yet.
- **No notification history storage** — deliberately deferred to an opt-in v0.2.0
  feature, not built silently.
- **Static model catalog** — no live `GET /v1beta/models` fetch yet; will drift as
  Google ships new models until v0.2.0.
- **OEM autostart allowlist** (Realme and others) cannot be reached programmatically;
  only the standard Android battery-optimization exemption is requested in-app.

## Sources consulted

- Direct authorship — this is a from-scratch build in the same session as this
  baseline, following forgecity-launcher's proven patterns (`AssistantSettingsStore`,
  `GeminiAudioTtsClient`, `NotificationDedupe`, `NotificationSpeechFilter`,
  `QuietHours`, `AssistantEventBridge`) as reference, not by copying its files.
- `E:\MyAgent\AGENTS.md`, `E:\MyAgent\workflow\CONSCIOUS.md`,
  `E:\MyAgent\GIT-RELEASE-MANAGEMENT.md`, `E:\MyAgent\workflow\aidlc\README.md`,
  `E:\MyAgent\workflow\review\REVIEWER-SIGNOFF.md` for the governance rules this
  baseline and release follow.
