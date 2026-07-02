# DSHub

Android app for DS허브.

## Version

Current release configuration:

- `versionCode`: `2`
- `versionName`: `1.0.2`

Google Play Console does not allow reusing an uploaded `versionCode`. If upload fails with "version code is already used", increase `versionCode` in `app/build.gradle.kts` and build the AAB again.

## Build

### Debug APK

```bash
./gradlew :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK

```bash
./gradlew :app:assembleRelease
```

Output:

```text
app/build/outputs/apk/release/app-release.apk
```

### Release App Bundle for Google Play

```bash
./gradlew :app:bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

This is the file to upload to Google Play Console.

## Release Signing

The release build currently expects the keystore at:

```text
~/dshub-release.jks
```

The signing config is defined in:

```text
app/build.gradle.kts
```

If the keystore is missing or the password/alias does not match, `assembleRelease` and `bundleRelease` will fail at the signing step.

## Deploy Script

`deploy.sh` builds an APK, installs it on the first connected ADB device, and launches the app.

Prerequisites:

- Android SDK platform tools installed
- `adb` available in PATH
- Android device or emulator connected
- USB debugging enabled for physical devices

### Install Debug Build

```bash
./deploy.sh
```

Same as:

```bash
./deploy.sh debug
```

### Install Release Build

```bash
./deploy.sh release
```

The script uses APK outputs, not AAB outputs. Use `./gradlew :app:bundleRelease` when preparing a Google Play upload.

## Useful Checks

Check connected devices:

```bash
adb devices -l
```

Check Git state:

```bash
git status -sb
```
