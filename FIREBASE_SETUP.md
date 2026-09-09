# Firebase AI Logic setup for Maria

The previous direct `google-genai-kotlin` client cannot be used in an Android APK with an API key; it intentionally throws a security exception. Maria now uses the official Firebase AI Logic Android SDK.

1. Open the [Firebase Console](https://console.firebase.google.com/) and create or select a project.
2. Add an Android app with this exact package name:
   `com.example.jarvis`
3. Download `google-services.json`.
4. Place it at `app/google-services.json` in this repository. Do not rename it or commit private debug tokens.
5. Enable Firebase AI Logic / Gemini Developer API in the Firebase console.
6. Configure App Check. Use the debug provider only for private development builds; use Play Integrity or another production provider for release builds.
7. Build and install the APK again.
8. In Maria, open `Settings → AI API Connection`, choose a model such as `gemini-2.5-flash`, tap `SAVE MODEL`, then tap `TEST CONNECTION`.

There is intentionally no Gemini API-key text box anymore. Firebase AI Logic authenticates through the Firebase project and App Check, preventing a long-lived provider key from being embedded in the APK.
