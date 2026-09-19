# Claim-Evidence Matrix

Use this document before writing the paper, poster, abstract, presentation, README, or Play Store text.

Status values:

- `implemented`: code path exists and builds.
- `tested`: verified on emulator or real device with recorded evidence.
- `partial`: code exists but evidence is incomplete.
- `unverified`: claim must not be presented as a result.
- `not implemented`: do not claim.

| Claim | Current status | Current evidence | Evidence still required |
|---|---|---|---|
| Android app builds successfully | tested | Debug APK and release AAB builds have passed locally. | Repeat after final code freeze. |
| App opens with setup before main workspace | implemented | Compose setup flow in `MainActivity.kt`. | Device screenshots and manual checklist. |
| Offline speech recognition is supported | implemented | Vosk dependency and bundled model flavors. | Real-phone recognition logs and WER/CER protocol. |
| App provides live captions | partial | Capture and transcript pipeline exists. | Real conversation test on target devices. |
| Capture continues during visible in-app work | partial | UI actions do not intentionally pause offline capture. | Numbered-speech test while scrolling/typing/generating reply. |
| Speaker-aware transcript is supported | partial | Local x-vector extraction and online clustering baseline. | DER/JER evaluation and multi-speaker phone tests. |
| Automatic speaker mode identifies people | not implemented | Labels are cluster/order based, not identity based. | Do not claim identity recognition. |
| Local conversation gist is generated | implemented | LiteRT-LM path and prompt flow exist. | Factuality, latency, memory, and multilingual evaluation. |
| Reply drafting is local and review-only | implemented | Prompt flow uses transcript, gist, and user intent; draft requires user action. | User study or qualitative evaluation. |
| Private AI Pack runs on-device | partial | Local file install and LiteRT-LM bridge exist. | Real-phone model install/inference proof and no-upload audit. |
| Fully offline operation | partial | Offline ASR and local model path exist. | Airplane-mode test, dependency/network audit, TTS evidence. |
| Better accessibility for deaf/HoH users | unverified | Product intent and UI exist. | Participant/user evaluation and accessibility test results. |
| Production-ready Play Store release | unverified | Release AAB builds. | Signing, privacy policy, data safety, crash testing, device matrix. |
| Medical or hearing-aid effectiveness | not implemented | Not a medical device. | Do not claim. |

## Paper claim rule

A paper sentence is allowed only when it can be traced to:

1. implemented code,
2. exact build/flavor,
3. test device,
4. procedure,
5. result,
6. limitation.

If any item is missing, write it as future work or design rationale, not as a result.
