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

## Pipeline (v0.1.0)

```
StatusBarNotification
  → CrierNotificationListenerService (dedupe, quiet-hours, allowlist filter)
  → CrierPipelineBus (in-process SharedFlow)
  → CrierForegroundService
      ├─ CallStateGate.isCallActive == true  → SpeechQueue.enqueue
      └─ CallStateGate.isCallActive == false → GeminiTtsClient.synthesize → GeminiAudioPlayer.play
  → CallStateGate flips to idle → SpeechQueue.drainReady() → speak each queued line
```

- `CrierNotificationListenerService` and `CrierForegroundService` run in the app's
  default process for v0.1.0 (not process-isolated). The foreground service's job is
  to keep that process at foreground priority so the OS is much less likely to kill
  it — see the Known limits section below for what that does and doesn't guarantee.
- `CrierPipelineBus` (SharedFlow) and `CrierStatusBus` (StateFlow) are in-process
  singletons, same pattern as forgecity-launcher's `AssistantEventBridge`. They only
  work because everything is one process; a future `:relay` process split (considered,
  not built — see ROADMAP) would need a real cross-process bridge (Messenger/AIDL or
  a signature-permission ContentProvider) to replace them.
- Never stores notification title/body in v0.1.0 — matches forgecity-launcher's
  existing privacy-first default. Opt-in encrypted history storage is a v0.2.0 item,
  not silently added.

## Call-aware queueing

`CallStateGate` wraps `TelephonyManager` (`TelephonyCallback` on API 31+,
`PhoneStateListener` below that — `minSdk` is 26). `RINGING` and `OFFHOOK` both count
as "wait" so a notification doesn't start talking over an incoming call's ring either.
`SpeechQueue` is a pure, Android-free bounded FIFO (max 20 items / 10 minute max age)
so a long call doesn't dump an unbounded backlog the moment it ends — unit-tested in
`SpeechQueueTest`.

## Config + secrets

`CrierSettingsStore` — SharedPreferences file `crier_settings`, Gemini API key
AES-GCM encrypted with an Android Keystore-backed 256-bit key (alias
`crier_gemini_api_key`), same shape as forgecity-launcher's
`AssistantSettingsStore.saveApiKey`. The key never leaves the Keystore; only the
ciphertext + IV live in prefs.

## Known limits (v0.1.0, honestly stated)

- **Not device-confirmed.** No physical Android device was attached to the build
  host for this release; only `gradlew assembleDebug` + JVM unit tests ran. See
  `docs/VERIFICATION.md`.
- **Foreground-service priority ≠ immunity from OEM battery managers.** Realme's
  (and other OEMs') aggressive background-kill / autostart-allowlist behavior is a
  known, previously-documented pain point in forgecity-launcher (repeated "Realme
  physical E2E pending" entries). `BatteryOptimization.requestExemptionIntent`
  requests the standard Android exemption; it cannot reach the OEM-specific
  autostart allowlist programmatically — that still needs a manual Settings nudge,
  called out in the app's own onboarding.
- **Single process.** `CrierNotificationListenerService` and `CrierForegroundService`
  share a process with `MainActivity`. A UI crash could, in principle, take the
  relay down with it. Process isolation (`android:process=":relay"`) was considered
  and deliberately deferred — see `docs/ROADMAP.md`.
- **Playground TTS-only.** STT and Live API are catalog entries, not wired to any
  network call yet.
