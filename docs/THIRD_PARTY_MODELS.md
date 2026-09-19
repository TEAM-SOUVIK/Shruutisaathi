# Third-Party Model Record

Date: 2026-09-12

This file records external model artifacts used or expected by ShrutiSaathi. Keep it current before release or paper submission.

## Local AI Pack

- Purpose: conversation gist and reply drafting.
- Runtime: LiteRT-LM Android.
- Current target artifact: Qwen3 0.6B no-think INT4 LiteRT-LM conversion.
- Installed app filename: `shrutisaathi-qwen3-06b-int4.litertlm`.
- Distribution mode: downloaded/imported after APK installation; not bundled in APK.
- Reason not bundled: APK size, install reliability, memory pressure, and Play Store package constraints.

Required release records:

- model source URL,
- model card,
- license,
- exact revision,
- artifact filename,
- byte size,
- SHA-256,
- download URL used by the app,
- any notice text required by the model license.

## Vosk speech models

- Purpose: offline speech recognition.
- Use: bundled by build flavor.
- Flavors:
  - `englishIn`
  - `englishUs`
  - `hindi`
  - `full`

Required release records:

- exact model names,
- upstream URLs,
- licenses,
- checksums,
- bundled paths,
- language/sample-rate assumptions.

## Vosk speaker model

- Artifact family: `vosk-model-spk-0.4`.
- Purpose: speaker embedding extraction for experimental local clustering.
- Use: session-level clustering only; not identity recognition.

Required release records:

- upstream URL,
- license,
- checksum,
- bundled path,
- notice text.

## Release rule

Before publishing or submitting final paper artifacts, every model used in the signed app must have a complete source, license, checksum, and notice record. If an item cannot be verified, do not claim it as validated or production-ready.
