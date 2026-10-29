# Orudesk mobile

The Android app (and, later, the iOS app) for Orudesk. Logic both apps share lives in `shared/`, written once in
Kotlin Multiplatform; each app draws its own screens: Jetpack Compose on Android, SwiftUI on iOS.

| Folder | What |
|---|---|
| `shared/` | The API client (Ktor), the login with its two-factor step, and the session (`AuthSession`) |
| `androidApp/` | The Android app: Compose screens, the refresh token encrypted with an Android Keystore key |

## Build and test

Needs JDK 17+ and the Android SDK (Android Studio installs it; set `ANDROID_HOME` or `sdk.dir` in
`local.properties`).

```bash
./gradlew :shared:jvmTest              # the shared logic's tests, on the JVM: no emulator needed
./gradlew :androidApp:assembleDebug    # androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

CI (`.github/workflows/mobile.yml`) runs both and keeps the debug APK for a week.

## Run against your API

Start the API (`docker compose ... --profile app up`, see the main README). The emulator reaches your computer
at `10.0.2.2`, so the app's default API is `http://10.0.2.2:8081`. For a phone or a real server:

```bash
./gradlew :androidApp:installDebug -Porudesk.apiBaseUrl=https://erp.example.org
```

Plain `http://` is allowed only for `10.0.2.2` and `localhost` (`res/xml/network_security_config.xml`).
