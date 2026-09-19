# Release signing for ShrutiSaathi v1.0

Google Play requires a signed Android App Bundle. This project is configured to build an unsigned release bundle by default and to sign it only when a local, ignored `release-keystore.properties` file is present.

## Recommended flow

1. Create an upload key using Android Studio or `keytool`.
2. Keep the keystore file outside version control.
3. Copy `release-keystore.properties.example` to `release-keystore.properties`.
4. Fill in the real keystore path, alias, and passwords.
5. Rebuild:

```powershell
.\gradlew.bat :app:bundleEnglishInRelease --console=plain
```

6. Verify signing:

```powershell
jarsigner -verify -verbose -certs app\build\outputs\bundle\englishInRelease\app-englishIn-release.aab
```

## Example local file

```properties
storeFile=keystores/shrutisaathi-upload.jks
storePassword=YOUR_PRIVATE_PASSWORD
keyAlias=shrutisaathi-upload
keyPassword=YOUR_PRIVATE_PASSWORD
```

Do not publish, share, or commit the real keystore or passwords. If the upload key is lost, future Play Store updates may require Play Console upload-key reset procedures.

