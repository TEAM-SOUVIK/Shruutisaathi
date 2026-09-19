# Android Studio and Device Testing

Date: 2026-09-12

## Open the project

Open:

```text
D:\Empower26\CodeX\AI-hearing-systems
```

Do not open only the `app` directory.

## Required toolchain

- Android Studio with Gradle JDK set to JetBrains Runtime 21.
- Android SDK platform 34 installed.
- Android SDK platform-tools installed.
- Build tools compatible with the installed Android Gradle Plugin.

PowerShell setup from the project root:

```powershell
$env:JAVA_HOME = 'C:\Users\Souvik_IIIT K\.jdks\jbr-21.0.11'
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=D:\Empower26\CodeX\AI-hearing-systems\app\build\java-tmp'
```

If the Java temp directory does not exist:

```powershell
New-Item -ItemType Directory -Path app\build\java-tmp -Force
```

## Build commands

Debug test/build:

```powershell
.\gradlew.bat :app:testEnglishInDebugUnitTest :app:assembleEnglishInDebug --console=plain
```

Release bundle:

```powershell
.\gradlew.bat :app:bundleEnglishInRelease --console=plain
```

## APK selection

Use:

- `app-englishIn-arm64-v8a-debug.apk` for Samsung, Honor, and most real Android phones/tablets.
- `app-englishIn-x86_64-debug.apk` for x86_64 Android emulator.

Do not install the x86_64 APK on a real ARM64 phone.

## First-run checklist

- [ ] App launches without crash.
- [ ] Intro logo is not cropped.
- [ ] Setup page appears first.
- [ ] Private AI Pack status is clear.
- [ ] Microphone permission prompt appears.
- [ ] Start button remains disabled until setup is valid.
- [ ] Participant count and names work.
- [ ] Light and dark themes are readable.
- [ ] Main page opens after valid setup.

## Live-caption checklist

- [ ] Start begins capture.
- [ ] Partial/final captions appear.
- [ ] Selected speaker label is applied.
- [ ] Pause, Resume, and Stop work.
- [ ] Caption correction edits the latest captured caption.
- [ ] Transcript Save, Share, and Delete behave correctly.
- [ ] Reset keeps setup and clears session.
- [ ] Stop returns to setup.

## Gist and reply checklist

- [ ] Gist appears after a short conversation.
- [ ] Gist does not invent people, company names, dates, or outcomes.
- [ ] Generate Reply uses typed intent.
- [ ] Regenerate does not reuse the previous reply as the new intent.
- [ ] Use Draft moves the draft into the response box.
- [ ] Reply is not automatically spoken or committed without user action.

## Continuous foreground capture test

Have another person read twelve short numbered sentences while the user:

1. scrolls,
2. types in Your Response,
3. updates gist,
4. generates reply,
5. saves transcript.

Pass condition: capture remains active and numbered utterances continue appending while the app is visible.

## Real-phone evidence to record

- Device model.
- Android version.
- APK flavor and ABI.
- Private AI Pack download time.
- Cold/warm gist latency.
- Cold/warm reply latency.
- Short transcript sample.
- Speaker-label success/failure notes.
- Crash, thermal, memory, or battery issues.
