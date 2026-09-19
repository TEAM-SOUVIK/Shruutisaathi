# Local AI Pack Setup

Date: 2026-09-12

ShrutiSaathi uses a local LiteRT-LM model for conversation gist and reply drafting. The model is not bundled inside the APK because that would make installation heavy and unreliable on normal phones.

## User workflow

1. Install the APK.
2. Open ShrutiSaathi.
3. On the first screen, install or download the Private AI Pack when prompted.
4. Keep the app open until installation completes.
5. Grant microphone permission.
6. Configure speakers.
7. Start the session.

The app should not open a browser for the normal model-download path. The intended experience is in-app progress, retry, and install status.

## Current model record

- Runtime: LiteRT-LM Android.
- Current lightweight model family: Qwen3 0.6B INT4 LiteRT-LM conversion.
- Installed filename in app storage: `shrutisaathi-qwen3-06b-int4.litertlm`.
- Purpose: concise gist and short reply draft generation.
- Inference mode: local device inference.

See [THIRD_PARTY_MODELS.md](THIRD_PARTY_MODELS.md) for source, license, hash, and artifact details.

## Behavior

### Gist generation

The prompt asks for one factual conversation gist, maximum 35 words. It should preserve the topic, latest question or decision, and important entities such as institute, company, job, campus, event, time, or name.

The prompt explicitly forbids:

- copied dialogue,
- invented facts,
- speaker labels,
- advice,
- reasoning text.

### Reply generation

The reply prompt uses:

- recent conversation context,
- current gist,
- user-intended words.

The typed text is treated as intent, not as text to continue. The output is a short review-only spoken draft. It is not automatically added to the transcript and is not automatically spoken.

## Failure behavior

If local generation fails, the app should:

- show a clear status message,
- preserve the transcript,
- preserve user-typed intent,
- avoid presenting fallback output as an AI result,
- allow retry.

TextRank or deterministic fallback output is only a recovery path, not the intended final gist system.

## Verification checklist

- [ ] Fresh install shows Private AI Pack status.
- [ ] Download progress is visible.
- [ ] Interrupted download can retry.
- [ ] Installed model survives app restart.
- [ ] Gist appears after conversation begins.
- [ ] Reply draft is generated only after tapping Generate Reply.
- [ ] Reply draft is review-only.
- [ ] No transcript is uploaded during local inference.
- [ ] Cold and warm gist/reply latency are recorded.
- [ ] Memory and thermal behavior are observed on real phones.
