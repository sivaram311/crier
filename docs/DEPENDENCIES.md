# Dependencies

| Item | Version |
|---|---|
| Android Gradle Plugin | 8.11.1 |
| Kotlin | 2.3.21 |
| KSP | 2.3.10 |
| Gradle wrapper | 8.14 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Compose BOM | 2025.07.00 |
| Room | 2.7.2 |
| WorkManager | 2.10.2 |
| Navigation Compose | 2.9.1 |
| Gemini API | `v1beta` REST (`generateContent`, `responseModalities: ["AUDIO"]`) — no SDK, plain `HttpsURLConnection` client |

No CSS dependency (this app has no login — see `docs/OPS.md` waiver). No Postgres.
No other MyAgent-tracked app dependency.

Versions chosen to match forgecity-launcher's proven-working pins (AGP/Kotlin/KSP/
Compose BOM/Room/WorkManager) rather than re-researching compatible versions from
scratch — same fleet, same known-good combination.

Per `E:\MyAgent\workflow\CONSCIOUS.md` rule 13, this app's release git tag is
recorded in `E:\MyAgent\workflow\deps\DEPENDENCY-MATRIX.md` at release time.
