# ShrutiSaathi v1.0

ShrutiSaathi is a local-first Android conversation-assistance application for deaf and hard-of-hearing users. It provides live captions, speaker-aware transcript organization, private conversation gist generation, and user-reviewed reply drafting.

This repository is the active Android product and research prototype for the EMPOWER 2026 paper work. It is not a medical device, hearing aid, emergency system, or production-certified accessibility tool.

## Open the project

Open this exact directory in Android Studio:

```text
D:\Empower26\CodeX\AI-hearing-systems
```

The correct root contains `settings.gradle.kts`, `gradlew.bat`, and the `app` module. Do not open only the `app` folder.

## Current application scope

- Kotlin Android application with Jetpack Compose UI.
- App name: `ShrutiSaathi v1.0`.
- Package: `com.aihearingassist`.
- Minimum SDK: 24.
- Compile SDK: 34.
- Target SDK: 36.
- Speech model flavors: `englishIn`, `englishUs`, `hindi`, and `full`.
- ABI-specific APKs: `arm64-v8a` for real phones and `x86_64` for emulator.
- Default workflow requires a one-time Private AI Pack installation before starting a session.
- Offline Vosk speech recognition is the primary capture path.
- Speaker setup supports predefined speaker names and experimental automatic local voice clustering.
- Gist and reply drafting use a local LiteRT-LM model when installed, with a deterministic fallback only for failure recovery.
- Transcripts are stored in app-private storage and excluded from Android backup configuration.

## Repository layout

```text
AI-hearing-systems/
├── app/                         Android application source, tests, resources, and model assets
├── docs/                        Product, testing, model, and research documentation
├── gradle/                      Gradle wrapper files
├── legacy/                      Historical prototype material only
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## Important documentation

- [Product requirements](docs/PRODUCT_REQUIREMENTS.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Android Studio and device testing](docs/ANDROID_STUDIO_TESTING.md)
- [Local AI model setup](docs/LOCAL_LLM_SETUP.md)
- [Speaker and gist methodology](docs/SPEAKER_AND_GIST_METHODOLOGY.md)
- [Claim-evidence matrix](docs/CLAIM_EVIDENCE_MATRIX.md)
- [Third-party model record](docs/THIRD_PARTY_MODELS.md)
- [Play Store release checklist](docs/play-store/RELEASE_CHECKLIST_V1_0.md)
- [Play Store listing draft](docs/play-store/STORE_LISTING.md)
- [Privacy policy draft](docs/play-store/PRIVACY_POLICY_DRAFT.md)
- [Data safety notes](docs/play-store/DATA_SAFETY_NOTES.md)
- [Release signing guide](docs/play-store/RELEASE_SIGNING.md)
- [v1.0 validation evidence](docs/play-store/VALIDATION_EVIDENCE_V1_0.md)

## Build commands

Use PowerShell from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Users\Souvik_IIIT K\.jdks\jbr-21.0.11'
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=D:\Empower26\CodeX\AI-hearing-systems\app\build\java-tmp'
.\gradlew.bat :app:testEnglishInDebugUnitTest :app:assembleEnglishInDebug --console=plain
```

Generate a Play Store bundle:

```powershell
.\gradlew.bat :app:bundleEnglishInRelease --console=plain
```

## Output locations

Real-phone debug APK:

```text
app\build\outputs\apk\englishIn\debug\app-englishIn-arm64-v8a-debug.apk
```

Emulator debug APK:

```text
app\build\outputs\apk\englishIn\debug\app-englishIn-x86_64-debug.apk
```

Release bundle:

```text
app\build\outputs\bundle\englishInRelease\app-englishIn-release.aab
```

## Research and release caution

Only implemented and tested behavior should be claimed in the app, paper, or presentation. Current work still needs controlled evaluation for word error rate, diarization error rate, gist factuality, reply usefulness, latency, memory, battery, thermal behavior, accessibility, and real-user acceptability.

Use `docs/CLAIM_EVIDENCE_MATRIX.md` before writing paper claims.
