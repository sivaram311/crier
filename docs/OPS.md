# Ops

**Sandbox DEV only.** This is an Android APK, not a hosted web service.

- No host listen port — nothing to reserve in `E:\MyAgent\workflow\ports\REGISTRY.md`.
- No Postgres — local Room DB only when notification history storage ships (v0.2.0+),
  scoped to on-device storage, not a shared machine instance.
- No CSS (Centralized Security System) — this app has no login; the only credential
  is a user-supplied Gemini API key, stored Keystore-encrypted on-device. Per
  `E:\MyAgent\CLAUDE.md` rule 5, a no-login local app may waive CSS — this note is
  that documented waiver.
- Distribution: public GitHub repo → tagged prerelease debug-signed APK asset,
  sideloaded via `adb install` (same model as forgecity-launcher).
- Outbound network: only `https://generativelanguage.googleapis.com` (Gemini API),
  using a user-supplied key. No other network calls in v0.1.0.

## Permissions and why

| Permission | Why |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Core feature — read posted notifications |
| `INTERNET` | Call the Gemini API |
| `RECEIVE_BOOT_COMPLETED` | Re-arm the foreground relay after reboot if enabled |
| `POST_NOTIFICATIONS` | Android 13+ requires this for the foreground-service notification |
| `READ_PHONE_STATE` | Call-state detection for queueing (`CallStateGate`) |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Persistent background relay (Android 14+ foreground service type) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ask to be exempted from Doze/App Standby |
| `RECORD_AUDIO` | Declared ahead of the v0.2.0 STT/Live API playground; unused in v0.1.0 |

## Release process followed

Per `E:\MyAgent\GIT-RELEASE-MANAGEMENT.md` §6: annotated `vX.Y.Z` tag, Reviewer
SIGN-OFF (`E:\MyAgent\workflow\review\`) before any push, GitHub public repo, tagged
release with the debug APK attached. No F:/G: promote for this app — see the CSS/DB/
port waivers above; there is no runtime environment to promote into.
