# ShrutiSaathi Product Requirements

Version: 1.0 development baseline  
Date: 2026-09-12  
Status: Android research prototype; production validation pending

## Product objective

ShrutiSaathi should help deaf and hard-of-hearing users follow nearby conversations through live captions, concise gist, speaker-aware transcript organization, and user-reviewed reply drafting. The default design direction is local-first processing and explicit user consent.

The application is not a hearing aid, medical device, diagnostic system, emergency tool, surveillance tool, or biometric speaker-identification product.

## Target users

- Deaf and hard-of-hearing users in small in-person conversations.
- Communication partners who are informed that the app is capturing speech.
- Researchers and developers evaluating assistive conversation workflows.

## Core requirements

| ID | Requirement | Acceptance condition |
|---|---|---|
| FR-001 | The app shall open with a setup screen before the main workspace. | User cannot start a session until required permission, consent, Private AI Pack, and speaker setup are complete. |
| FR-002 | The app shall request microphone permission clearly. | Missing permission blocks session start and shows an actionable error. |
| FR-003 | The app shall support predefined participant names. | Default names are `Speaker 1`, `Speaker 2`, etc., and can be edited before starting. |
| FR-004 | The app shall support experimental automatic speaker clustering. | Automatic mode uses local voice evidence and must not claim biometric identity. |
| FR-005 | The app shall provide live captions. | Captions append to the transcript while capture is active and the app remains foreground-visible. |
| FR-006 | Capture shall continue during in-app reading, scrolling, typing, gist update, reply generation, save, and delete actions. | Offline capture is not intentionally paused by those visible in-app actions. |
| FR-007 | Capture shall stop or suspend when the app leaves the foreground. | The UI reports the foreground limitation and attempts safe recovery when returning. |
| FR-008 | The app shall provide a concise conversation gist. | Private AI Pack is required for normal sessions; fallback text is only a recovery path. |
| FR-009 | The app shall generate user-reviewed reply drafts. | Generated reply never auto-speaks or auto-commits to transcript without user action. |
| FR-010 | Reply generation shall use user intent, transcript context, and gist. | Prompting treats typed text as intent, not continuation text. |
| FR-011 | The transcript shall be saveable, shareable, and deletable. | Save, Share, and Delete controls are visible, labelled, and guarded where needed. |
| FR-012 | Reset and Stop shall be distinct. | Reset clears the current session while keeping setup; Stop clears the session and returns to setup. |
| FR-013 | UI shall support light and dark themes. | Both themes remain readable and visually distinct on phone and tablet screens. |
| FR-014 | The app shall preserve privacy-oriented wording. | Generated, recognized, user-authored, and review-marked content are distinguishable. |
| FR-015 | The app shall support Android build flavors. | `englishIn`, `englishUs`, `hindi`, and `full` variants remain buildable. |

## Non-functional requirements

| ID | Requirement | Acceptance condition |
|---|---|---|
| NFR-001 | App-private transcript storage. | Android backup/data extraction rules exclude transcript data. |
| NFR-002 | Local-first model path. | Model setup and documentation identify local inference behavior and limitations. |
| NFR-003 | Device compatibility. | ARM64 APK installs on target phones; x86_64 APK installs on emulator. |
| NFR-004 | Accessibility readiness. | UI uses readable hierarchy, labels, contrast, and scalable layout; TalkBack testing remains required. |
| NFR-005 | Evidence-based claims. | Any paper/product claim must map to code, device, protocol, result, and limitation. |
| NFR-006 | Performance transparency. | Latency, memory, thermal, battery, and crash behavior must be measured on target devices before release claims. |

## Out of scope for the current baseline

- Medical/hearing-aid certification.
- Biometric identity recognition.
- Guaranteed overlapped-speech separation.
- Lossless background capture.
- Emergency-call support.
- Validated clinical outcomes.
- Production subscription/payment flow.

## Acceptance gates

The current baseline should be treated as accepted only after:

1. Unit tests pass.
2. Debug APK builds.
3. Release AAB builds.
4. App installs on emulator and real ARM64 phone.
5. Private AI Pack downloads/installs successfully.
6. Live caption, gist, reply, speaker label, reset, stop, save, share, and delete workflows pass manual smoke tests.
7. Evidence matrix is updated before any paper claim is written.
