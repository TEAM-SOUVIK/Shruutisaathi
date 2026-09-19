# ShrutiSaathi v1.0 release checklist

## Build identity

- App label: ShrutiSaathi v1.0
- Package/application ID: `com.aihearingassist`
- Version code: `1`
- Version name: `1.0`
- Minimum SDK: 24
- Target SDK: 36
- Primary Play artifact: `app/build/outputs/bundle/englishInRelease/app-englishIn-release.aab`
- Signing: configured through ignored local `release-keystore.properties`; create the publisher upload key before final Play upload.

## Completed local checks

- Unit tests across build variants.
- English India debug APK build.
- English India release lint.
- English India release AAB build with R8/resource shrinking.

## Manual checks before Play upload

- Install the `arm64-v8a` debug APK on a real phone.
- Start a new session with two predefined speaker labels.
- Confirm speaker labels auto-advance after captured turns.
- Confirm transcript correction updates the correct turn.
- Install/download the Private AI Pack.
- Test the meeting example:
  - Speaker One: "Can we meet at three?"
  - Speaker Two: "I have a class then. Can we make it four?"
  - Speaker One: "Yes, four works."
  - Expected gist: "The meeting is now at four o'clock."
  - User intent: "four okay, send room"
  - Expected reply: "Four o'clock works for me. Please send me the room number."
- Confirm Reset Session clears the local working session.
- Confirm Stop Current Session stops capture and returns to setup.
- Confirm app remains usable after rotation/background/foreground on the target devices.

## Play Console items still requiring publisher action

- Create/select Play Console app.
- Add final privacy policy URL.
- Complete Data safety form from `DATA_SAFETY_NOTES.md`.
- Complete content rating questionnaire.
- Add final screenshots for phone and tablet.
- Upload feature graphic if desired.
- Configure app category and contact details.
- Use Play App Signing and the publisher's upload key.
- Rebuild the AAB after adding `release-keystore.properties`, then verify `jarsigner` reports a valid signature.
- Choose production/internal/closed testing track.

## Policy caution

As of the current Google Play target API policy, new Android apps and updates submitted on or after August 31, 2026 must target Android 16 / API level 36 or higher. This project sets `targetSdk = 36`.
