# Jarvis Agent Android Project

This project consolidates Phases 1–7: root shell execution, voice service, Gemini tools, local builder preview, Auto-Fix, accessibility screen context, and lifecycle orchestration.

## Setup

1. Open this directory in Android Studio.
2. Install Android SDK 35 and use JDK 17.
3. Set a Gemini credential through a secure backend or internal prototype configuration. Do not commit an API key.
4. Grant `RECORD_AUDIO`, overlay access, notification permission, and battery settings as appropriate.
5. Enable the Jarvis accessibility service manually in Android Settings.
6. Test root functions only on a rooted device with explicit superuser authorization.

## Build

```bash
./gradlew assembleDebug
```

Install the debug APK with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The sandbox used to generate this project did not have Android SDK, Gradle, or ADB installed, so the build must be run in Android Studio or an Android build environment.
