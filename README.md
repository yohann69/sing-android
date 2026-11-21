# Sing! for Android

The Android remote for Sing!: find the karaoke Mac on the Wi‑Fi, pair with the code or the TV's QR, then search, queue, follow the lyrics and control the party. Kotlin, Jetpack Compose, Material 3. It speaks the same protocol as the iPad app (see `protocol/README.md` in sing-apple).

## Build

Needs the Android SDK (platform 37) and a JDK 17+ (Android Studio's bundled one works).

```bash
./gradlew assembleRelease
```

The APK lands in `app/build/outputs/apk/release/`. Install it with `adb install` or by opening it on the device; no Play Store needed.

`./gradlew testDebugUnitTest` runs the contract tests against payloads recorded from a real Mac.

Keep this folder in a path without `!`: the Kotlin compiler cannot read sources from such paths.

## License

MIT
