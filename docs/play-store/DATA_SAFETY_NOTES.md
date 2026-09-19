# Google Play Data safety notes for ShrutiSaathi v1.0

Use this as a preparation worksheet for Play Console. Final answers must be entered by the publisher in Play Console and checked against the exact release build.

## Permissions

- Microphone: required for live caption capture.
- Internet: used for optional one-time Private AI Pack download.

## Data categories likely involved

| Data type | App behavior | Shared off device by ShrutiSaathi backend? | Notes |
|---|---|---:|---|
| Audio | Microphone audio is processed for live captions while capture is active. | No dedicated ShrutiSaathi backend upload in v1.0 design. | Android/system recognizer behavior may vary if Enhanced mode is used. Private mode uses bundled/offline capture. |
| App activity / user-generated content | Transcript, gist, typed intent, and generated reply draft. | No dedicated ShrutiSaathi backend upload in v1.0 design. | Stored in app-private storage; user can share transcript manually via Android share sheet. |
| App info and performance | Local model download progress/settings. | No dedicated ShrutiSaathi backend upload in v1.0 design. | Standard Play/Android diagnostics may be separately governed by Google/device settings. |

## Security and privacy claims to keep conservative

- Say "local-first" or "on-device after model installation".
- Do not say "100% accurate", "medical grade", "certified", or "guaranteed private".
- If Enhanced/system speech recognition is enabled in the final UI, disclose that it may use the device/vendor speech service.
- If publishing only Private mode, state that speech capture uses local bundled models.

## Deletion controls

- Delete transcript: removes committed transcript history.
- Reset session: clears transcript, summary, response draft, generated reply, and local session history while keeping speaker setup.
- Stop current session: stops capture and returns to setup.

## Backup

The release manifest references backup rules that exclude `ai_hearing_assist_conversation.xml` from cloud backup and device transfer.

