# ShrutiSaathi v1.0 validation evidence

Generated on: 2026-09-17

## Commands run

```powershell
$env:JAVA_HOME = 'C:\Users\Souvik_IIIT K\.jdks\jbr-21.0.11'
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=D:\Empower26\CodeX\AI-hearing-systems\app\build\java-tmp'

.\gradlew.bat :app:bundleEnglishInRelease --console=plain
.\gradlew.bat :app:test :app:assembleEnglishInDebug :app:lintEnglishInRelease --console=plain
```

## Results

- `:app:bundleEnglishInRelease`: passed.
- `:app:test`: passed across englishIn, englishUs, full, and hindi debug/release unit-test variants.
- `:app:assembleEnglishInDebug`: passed.
- `:app:lintEnglishInRelease`: passed.
- Post-signing-config smoke check: `:app:testEnglishInDebugUnitTest :app:bundleEnglishInRelease` passed.

## Generated artifacts

- Debug APK for real ARM64 phones:
  `app/build/outputs/apk/englishIn/debug/app-englishIn-arm64-v8a-debug.apk`
- Debug APK for x86_64 emulator:
  `app/build/outputs/apk/englishIn/debug/app-englishIn-x86_64-debug.apk`
- Release AAB:
  `app/build/outputs/bundle/englishInRelease/app-englishIn-release.aab`
- Lint report:
  `app/build/reports/lint-results-englishInRelease.html`
- Unit test reports:
  `app/build/reports/tests/`

## Regression areas covered by tests

- Conversation transcript persistence and reset behavior.
- Duplicate and same-speaker fragment handling.
- Speaker-turn tracker behavior.
- Offline gist generation, including the meeting decision example.
- Reply generation fallback, including shorthand intent expansion.
- Local text prompt cleaning and grounding checks.
- Summary coordinator batching/cancellation behavior.
- Memory policy for local model loading.

## Remaining validation that needs physical devices

- Real microphone quality across target phones/tablets.
- Battery, thermal, and RAM measurements during a long session.
- AI Pack download reliability on slow networks.
- Speaker separation accuracy on real multi-speaker audio.
- Accessibility review with users and ethics/consent process if used in a study.

## Signing status

The Gradle release signing configuration is prepared but intentionally reads secrets from ignored local file `release-keystore.properties`. If that file is not present, the generated AAB is not upload-key signed. Create the publisher upload key, fill `release-keystore.properties`, rebuild `:app:bundleEnglishInRelease`, and verify with `jarsigner` before Play Console upload.
