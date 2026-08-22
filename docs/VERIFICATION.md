# Verification

## Verification History

### v0.1.0 – v0.1.4
- **Unit Testing**: Passed all unit tests in `NotificationDedupeTest`, `NotificationSpeechFilterTest`, `QuietHoursTest`, `SpeechQueueTest`.
- **Runtime Fixes**: Added `READ_PHONE_STATE` runtime permission request, thread-safe synchronization for `SpeechQueue`, lock-screen speech privacy gate (`speakWhenLocked`), WAV/PCM streaming audio decoding, and in-app back navigation.

### v0.1.5
- **Live Diagnostics (`CrierLogBus`)**: Validated in-memory log streaming with request payloads and response bodies.
- **Dynamic Models (`GeminiModelCatalog`)**: Verified `GET /v1beta/models` live query and parsing.
- **System Prompt Support**: Validated `systemInstruction` injection and full notification text extraction.

### v0.1.6
- **Build**: `.\gradlew.bat testDebugUnitTest assembleDebug` passing with zero errors.
- **TTS Instruction Compatibility**: Verified that `-tts` models (`gemini-3.1-flash-tts-preview`, `gemini-2.5-flash-preview-tts`, `gemini-2.5-pro-preview-tts`) which reject `systemInstruction` with `HTTP 400 Developer instruction is not enabled for this model` are handled with proactive prompt inlining and auto-recovery fallback.
- **Notification Filter Tests**: Extended `NotificationSpeechFilterTest` to verify that ongoing sticky notifications and foreground service notifications are filtered by default but allowed when the user toggles the respective setting off.
- **Checksum**: Debug APK SHA-256 is `6830656F3EDEA4F0C964593C7F190517564B4F9DEC485B5331EE8C3F16615226`.
