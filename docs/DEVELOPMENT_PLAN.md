# ShrutiSaathi Development Plan

Date: 2026-09-12  
Scope: Android application and EMPOWER 2026 evidence preparation

## Current milestone

The current goal is to stabilize an Android research prototype that is credible enough for paper writing, demo recording, and structured evaluation.

## Development rules

1. Keep the Android project root at `D:\Empower26\CodeX\AI-hearing-systems`.
2. Preserve a working build before major changes.
3. Keep product claims separate from research hypotheses.
4. Do not claim accuracy, privacy, latency, accessibility, or user benefit without evidence.
5. Test on emulator for UI flow, then on real phone for microphone, model, TTS, latency, memory, and thermal behavior.

## Completed implementation areas

- Compose-based setup screen and main workspace.
- Light and dark visual themes.
- App branding, launcher icon, and intro animation.
- Offline speech-recognition workflow.
- Participant setup with predefined and automatic modes.
- Online speaker clustering baseline using local voice embeddings.
- Live caption console and correction workflow.
- Conversation gist panel.
- User response panel with next-word suggestions.
- Local reply-generation prompt flow.
- Transcript save/share/delete actions.
- Compact persistent session controls.
- Local model download/import workflow.
- Unit tests for prompt, summary, transcript, and policy logic.
- Release AAB generation.

## Immediate next tasks

| Priority | Task | Output |
|---|---|---|
| P0 | Real-phone smoke test on Samsung A15 and Honor tablet. | Pass/fail log with screenshots and timing notes. |
| P0 | Validate Private AI Pack download on real network. | Model install time, retry behavior, storage used. |
| P0 | Record gist and reply quality examples. | Small qualitative table for paper discussion. |
| P0 | Measure local inference latency. | Cold/warm gist and reply timings. |
| P1 | Run two-speaker scripted test. | Speaker-label stability notes and failure cases. |
| P1 | Accessibility pass. | Font scale, TalkBack, contrast, touch target notes. |
| P1 | Battery/thermal observation. | 10-15 minute capture session notes. |

## Paper-writing preparation

Before drafting claims, collect:

- Device model, Android version, RAM, and CPU.
- APK flavor and build date.
- Speech language/model used.
- Example transcript, gist, and reply outputs.
- Latency logs for gist and reply.
- Speaker-separation success/failure examples.
- Known limitations and ethical safeguards.

## Future engineering work

- Move capture into a foreground service with explicit consent and notification.
- Add robust session database with retention controls.
- Add encryption decision and migration tests.
- Add proper diarization evaluation with DER/JER.
- Add WER/CER evaluation protocol.
- Improve prompt/result evaluation for multilingual conversations.
- Add CI and signed release pipeline.
- Add Play Store privacy/data-safety documentation.
