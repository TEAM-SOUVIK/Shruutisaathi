# ShrutiSaathi v1.0 Privacy Policy draft

Effective date: TODO before publication

ShrutiSaathi is a local-first Android conversation-assistance app. This privacy policy draft explains the intended data behavior for the v1.0 build. Replace TODO items with the publisher's legal name, contact details, hosting URL, and jurisdiction before publishing.

## Data processed by the app

ShrutiSaathi may process:

- Microphone audio while live caption capture is active.
- Generated caption text.
- Speaker labels configured by the user.
- Conversation transcript text.
- Conversation gist text.
- User-typed reply intent and generated reply drafts.
- Local model download progress and local app settings.

## Purpose of processing

The app uses this data to:

- Capture live captions.
- Organize conversation turns by speaker label.
- Generate a short conversation gist.
- Draft a reply from the user's own typed intent and the current conversation context.
- Save and restore the current local session when the user chooses to continue it.

## Local storage

Conversation transcripts and settings are stored in app-private storage on the user's device. The app's backup configuration excludes saved conversation data from Android cloud backup and device-transfer backup.

## Network access

The app uses internet access for the optional one-time Private AI Pack download. After the model is installed, the intended gist and reply workflow runs locally on the device. The app does not require sending conversation transcripts to a remote server for gist or reply generation.

## Audio handling

Microphone audio is used for live caption capture while the user starts a session. The app does not intentionally upload live microphone audio to a ShrutiSaathi server.

## Sharing

If the user chooses to share a transcript using Android's share sheet, the selected external app receives the shared text. That external app's own privacy policy applies.

## Data deletion

Users can delete the current transcript and reset or stop the current session inside the app. Uninstalling the app removes app-private local data from the device.

## Children's privacy

ShrutiSaathi v1.0 is not directed to children. If publishing for general availability, configure the Play Console target audience and content rating accurately.

## Contact

Publisher: TODO

Contact email: TODO

Privacy policy URL: TODO

## Limitations

ShrutiSaathi is not a medical device, hearing aid, emergency service, or certified legal transcription tool. Captions, summaries, and reply drafts can be inaccurate and should be reviewed by the user.

