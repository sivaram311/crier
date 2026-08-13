# Verification

## v0.1.0

- **Build:** `gradlew assembleDebug` — see build log referenced in the release
  ACTIVITY-LOG entry. JVM unit tests (`NotificationDedupeTest`,
  `NotificationSpeechFilterTest`, `QuietHoursTest`, `SpeechQueueTest`) run via
  `gradlew testDebugUnitTest`.
- **Physical device: PENDING.** No Android device was attached to the build host for
  this release — same constraint documented repeatedly in forgecity-launcher's own
  VERIFICATION history ("Realme physical E2E pending"). Nothing in this release is
  claimed as device-confirmed.
- **Not yet manually verified:**
  - Notification-listener actually receives and speaks a real notification
  - Call-state gate correctly detects `RINGING`/`OFFHOOK` and queues/drains on a real
    call (emulator call simulation would partially cover this)
  - Foreground-service survival under real OEM battery management (Realme especially)
  - Battery-optimization exemption intent flow end-to-end
  - AudioTrack playback routing (earpiece vs. speaker) on a real device

Anyone installing the sideloaded APK is the first real-device confirmation. Report
back and this file gets updated — same pattern as forgecity-launcher's release notes.
