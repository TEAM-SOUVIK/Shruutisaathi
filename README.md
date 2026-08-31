![Shruutisaathi banner](docs/assets/shruutisaathi-banner.png)

# Shruutisaathi

Shruutisaathi is a local-first conversation-assistance application for deaf and
hard-of-hearing users. The Android application is the canonical product in this
repository. It is currently a development alpha, not a medical device and not a
production release.

## Open the project

Open this exact folder in Android Studio:

```text
D:\Empower26\CodeX\AI-hearing-systems
```

This repository root contains `settings.gradle.kts`. Do not open only the
`app` directory.

## Repository layout

```text
AI-hearing-systems/
├── app/                         Android application and bundled Vosk models
├── docs/
│   ├── ANDROID_STUDIO_TESTING.md
│   ├── CLAIM_EVIDENCE_MATRIX.md
│   ├── DEVELOPMENT_PLAN.md
│   ├── PRODUCT_REQUIREMENTS.md
│   └── reference/               Historical source documents
├── legacy/web-prototype/        Non-production Flask/Whisper/BART prototype
├── gradle/                      Gradle wrapper files
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Current honest baseline

- Kotlin and Jetpack Compose Android UI.
- Offline Vosk recognition is selected by default.
- The optional Android system speech service is labelled as potentially
  network-backed.
- A Session Setup page collects microphone consent and predefined participant
  names before the main screen opens.
- Captions support either manually selected participant names or Automatic AI
  separation using the bundled Vosk x-vector speaker model and online cosine
  clustering. Automatic AI requires at least two voice clusters. Automatic
  labels follow first-heard voice order; they are not biometric identities.
- Local transcript save, restore, clear, and Android share-sheet export.
- An automatically refreshed, two-line conversation gist produced by an
  on-device TextRank-style sentence-centrality and topic-salience baseline.
- Deterministic on-device next-word suggestions that remain hidden until the
  user types; partial-word completions replace the partial word.
- A review-only generated-reply baseline that conservatively cleans the user's
  intended words. It is not a local LLM and does not invent conversation facts.
- A complete selectable transcript with labelled Save, Share, and Delete
  actions, plus separate Reset Session and Stop Current Session controls.
- In Offline mode, foreground microphone capture remains active while the user
  reads or scrolls, types a response, updates the summary, generates/reviews a
  reply, saves, or deletes committed transcript history.
- Speaker selection is latched at the first partial result, so changing the
  selected participant during speech affects the next utterance rather than
  relabelling the utterance already in progress.
- Generated replies record the transcript turn on which they were based and are
  marked stale when a new caption arrives.
- TTS does not pause recognition. A possible exact playback echo is retained and
  marked for review; earphones are recommended while testing simultaneous reply
  playback and nearby speech.
- Typed replies are configured to select only an installed TTS voice that
  reports that it does not require a network connection.
- A lifecycle guard stops microphone capture when the app is no longer visible
  and attempts to resume it when the active session returns to the foreground.

The speaker-separation, TTS, and lifecycle safeguards remain device-unverified
until the test checklist passes in LDPlayer and on a real phone.

## Android build variants

| Variant | Bundled model | Recommended use |
|---|---|---|
| `englishUsDebug` | US English | First Android Studio and LDPlayer baseline |
| `englishInDebug` | Indian English | Real-phone validation after the baseline |
| `hindiDebug` | Hindi | Hindi and experimental Hinglish/Bhojpuri paths |
| `fullDebug` | All three models | Size and multi-model testing only |

The Indian-English recognizer source is configured for its model's 8-kHz input;
the US-English and Hindi configurations use 16 kHz. Some devices may need an
explicit resampling fallback, so every flavor still requires real-device model
load, accuracy, and latency testing.

## First build

Use the configured JBR 21, Android SDK Platform 34, Build-Tools 36.0.0, and Android SDK
Platform-Tools. Then select `englishUsDebug` in Android Studio's Build Variants
window and run the `app` configuration.

Command-line verification from the repository root:

```powershell
.\gradlew.bat testEnglishUsDebugUnitTest assembleEnglishUsDebug
```

Detailed setup and test steps are in
[`docs/ANDROID_STUDIO_TESTING.md`](docs/ANDROID_STUDIO_TESTING.md).

## Engineering documents

- [`docs/PRODUCT_REQUIREMENTS.md`](docs/PRODUCT_REQUIREMENTS.md) defines the
  product scope and acceptance requirements.
- [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md) defines development and
  research gates.
- [`docs/CLAIM_EVIDENCE_MATRIX.md`](docs/CLAIM_EVIDENCE_MATRIX.md) prevents paper
  or product claims from getting ahead of the implemented evidence.
- [`docs/SPEAKER_AND_GIST_METHODOLOGY.md`](docs/SPEAKER_AND_GIST_METHODOLOGY.md)
  records the implemented research pipeline, parameters, limitations, and
  evaluation plan.
- [`docs/LOCAL_LLM_SETUP.md`](docs/LOCAL_LLM_SETUP.md) explains how to install
  the required LiteRT-LM Gemma model used for local generative summaries.

## Known limitations

- Automatic speaker separation is utterance-level online clustering, not
  biometric identity. It has not yet been calibrated on the target phones or
  evaluated with DER/JER, and simultaneous overlapping voices are not separated.
- Continuous capture currently means foreground capture while ShrutiSaathi is
  visible. Speech during Home, screen-lock, another app, a permission dialog, or
  a share chooser is not guaranteed; an Android foreground capture service is
  future work.
- Android System recognition is best-effort because the provider can end and
  restart sessions. The bundled Offline mode is the continuous-capture target.
- Keyboard voice typing can compete for the microphone and is not part of the
  continuous-capture guarantee.
- No trained context-aware word-prediction model.
- Local generative summaries require a separately installed `.litertlm` Gemma
  model before the main session starts. TextRank remains only as an emergency
  fallback if installed-model inference fails.
- No local LLM reply model; the current generated reply is a deterministic
  safety baseline for testing the workflow.
- No validated application-level WER, DER, latency, memory, battery, thermal,
  accessibility, or user-study results yet.
- Transcripts currently use a simple single-session local store. Session history,
  encryption, retention controls, and migrations are future work.
- LDPlayer is suitable for UI and workflow checks, but microphone routing,
  offline speech quality, TTS availability, battery, thermal behavior, and
  accessibility must be tested on real phones.

## Legacy web prototype

The Flask/Whisper/BART/gTTS experiment is isolated under
[`legacy/web-prototype`](legacy/web-prototype). It is retained for historical
reference only and must not be deployed or used as evidence for the Android app.
