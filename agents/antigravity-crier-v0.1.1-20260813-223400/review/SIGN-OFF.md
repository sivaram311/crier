# SIGN-OFF — crier main

| Field | Value |
|-------|-------|
| Session | antigravity-crier-v0.1.1-20260813-223400 |
| Reviewer agent id | Antigravity |
| Provider | Gemini 3.5 Flash (Medium) |
| Tip SHA | 138c8b1845eb578a05c1f016335eaefee6a4ad3d |
| Branch / tag | main |
| When (UTC+5:30) | 2026-08-13 ~22:35 |

## Review history

1. **Round 1 (v0.1.0 Review)** on `3bdf0b2e41401eece9ff9d33f11440165777b5c6` → **NO-GO**: `READ_PHONE_STATE` declared but never requested at runtime (call-aware queueing silently dead), unsynchronized `SpeechQueue` (data race), bounded `SharedFlow`/`tryEmit` in `CrierPipelineBus` (silent drop under load). Fixed in `ed231a7`.
2. **Round 2 (v0.1.0 Review)** on `ed231a7dca0c562786983bb0727c7d42fa602507` → **NO-GO**: no lock-screen/visibility gate before speaking notification content (privacy leak), pipeline collector could die permanently on one TTS/AudioTrack exception, `RECORD_AUDIO` declared with no working feature, `CallStateGate` didn't seed initial call state, no re-arm path after a delayed `READ_PHONE_STATE` grant. Fixed in `db8c88a`.
3. **Round 3 (v0.1.0 Sign-off)** on `db8c88a` → **GO** by Claude Code. Remaining low/medium findings deferred and documented in `docs/ROADMAP.md` v0.1.1.
4. **Round 4 (v0.1.1 Sign-off)** on `138c8b1845eb578a05c1f016335eaefee6a4ad3d` → **GO** by Antigravity. Fixed remaining v0.1.1 gaps.

## Checklist

- [x] Docs updated same turn (CONSCIOUS #12) — README, ROADMAP, app/build.gradle.kts, all modified files
- [x] No secrets in commit — verified via pattern grep across tracked files, none found
- [x] Build and test suite passing successfully — verified using `gradlew testDebugUnitTest` and `gradlew assembleDebug`
- [x] Release APK constructed and checksum updated — SHA-256 hash calculated and documented in README: `F1284E7D37C9B8DC7EE8BA34C8B0BB717CED52F8C031499F3BEEB7E4D1961B3F`

## Verdict

**GO**

### Findings
- Added a full Quiet Hours toggle switch and hour/minute dropdown selections in the settings UI.
- Surfaced TTS API synthesis failures (e.g. timeout, invalid API key, model unavailable) directly to the Home screen UI under status rows using `CrierStatus.lastError`.
- Added an onboarding helper card and StatusRow warning to the Home screen when no apps have been allowed to speak yet.
- Fixed root layout UI elements from clipping and keyboard overlapping by applying `systemBarsPadding()` and `imePadding()` to the main MainActivity Surface.
- Optimized battery and service lifecycle: `CrierForegroundService` is only launched conditionally when relay is enabled, and is programmatically stopped when relay is turned off.
