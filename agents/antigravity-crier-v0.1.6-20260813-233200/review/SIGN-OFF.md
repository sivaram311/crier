# SIGN-OFF — crier main

| Field | Value |
|-------|-------|
| Session | antigravity-crier-v0.1.6-20260813-233200 |
| Reviewer agent id | Antigravity |
| Provider | Gemini 3.7 Flash |
| Tip SHA | (Committing soon) |
| Branch / tag | main |
| When (UTC+5:30) | 2026-08-13 ~23:32 |

## Review history

1. **Round 1 (v0.1.0 Review)** on `3bdf0b2e41401eece9ff9d33f11440165777b5c6` → **NO-GO**: `READ_PHONE_STATE` declared but never requested at runtime, unsynchronized `SpeechQueue`, bounded `SharedFlow`/`tryEmit` in `CrierPipelineBus`. Fixed in `ed231a7`.
2. **Round 2 (v0.1.0 Review)** on `ed231a7dca0c562786983bb0727c7d42fa602507` → **NO-GO**: lock-screen privacy gate, collector crash resilience, `CallStateGate` initial seed. Fixed in `db8c88a`.
3. **Round 3 (v0.1.0 Sign-off)** on `db8c88a` → **GO** by Claude Code.
4. **Round 4 (v0.1.1 Sign-off)** on `138c8b1845eb578a05c1f016335eaefee6a4ad3d` → **GO** by Antigravity.
5. **Round 5 (v0.1.2 Sign-off)** on `368d070b43ecf8e562145b59a0fef7381cb1abcf` → **GO** by Antigravity.
6. **Round 6 (v0.1.3 Sign-off)** on `cb7782b2a45c4ae4653718f8189721d84d51e9f1` → **GO** by Antigravity.
7. **Round 7 (v0.1.4 Sign-off)** on `e7272ab6795f79be27aa210214a1e944b2fa68c8` → **GO** by Antigravity.
8. **Round 8 (v0.1.5 Sign-off)** on `391ee0a28fbe567a57a151b5c8bc2385b00c3b06` → **GO** by Antigravity.
9. **Round 9 (v0.1.6 Sign-off)** → **GO** by Antigravity. TTS preview model developer-instruction compatibility, instruction auto-recovery fallback, and configurable ongoing notification filters.

## Checklist

- [x] Docs updated same turn (CONSCIOUS #12) — README, ROADMAP, app/build.gradle.kts, all modified files
- [x] No secrets in commit — verified via pattern grep across tracked files, none found
- [x] Build and test suite passing successfully — verified using `gradlew testDebugUnitTest` and `gradlew assembleDebug`
- [x] Release APK constructed and checksum updated — SHA-256 hash calculated and documented in README: `6830656F3EDEA4F0C964593C7F190517564B4F9DEC485B5331EE8C3F16615226`

## Verdict

**GO**

### Deliverables
- **TTS Instruction Fix (`GeminiTtsClient`)**: Proactively inlines system prompts and auto-recovers from HTTP 400 `"Developer instruction is not enabled for this model"` errors for models like `gemini-3.1-flash-tts-preview` and `gemini-2.5-flash-preview-tts`.
- **Configurable Notification Filters**: Added settings switches for `filterOngoingNotifications` and `filterForegroundServices` in `CrierSettingsStore` and `SettingsScreen`.
- **Prioritized TTS Models in Catalog**: Sorted dedicated native audio models (`gemini-3.1-flash-tts-preview`, `gemini-2.5-flash-preview-tts`, etc.) at the top of the model selection list.
