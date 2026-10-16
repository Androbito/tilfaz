# Releasing Tilfaz

Target pipeline (from the `mobile-release-pipeline` skill): PR checks -> main builds a debug APK ->
tag `v*` builds a signed release and uploads to Play internal testing with a staged rollout.

## What exists today

- `.github/workflows/android-ci.yml`: ktlint, lint, unit tests, screenshot tests, debug APK, coverage on every PR and on main.

## To add before the first store build

1. Create an upload keystore and store it as CI secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
   Wire them in `app/build.gradle.kts` (`signingConfigs.create("release")` reading `System.getenv`). Never commit the keystore.
2. Play Console service account JSON as `PLAY_SERVICE_ACCOUNT_JSON`; Fastlane `supply` lane with `track: internal`, then `userFraction` for staged rollout.
3. Version from the tag: `./gradlew -PversionName=${TAG#v} -PversionCode=$GITHUB_RUN_NUMBER :app:bundleRelease`.
4. Crash reporting (Firebase Crashlytics or Sentry) behind a `CrashReporter` interface so debug builds use a no-op.
5. Forced update: remote minimum version check at launch (Remote Config) with a blocking screen below it.

## Android TV store requirements to keep in mind

- Leanback launcher intent and banner are in the manifest already; the banner must stay 320x180.
- No touchscreen requirement (`android.hardware.touchscreen required=false`).
- Every screen must be fully operable with a D-pad: PR reviews should check focus order on new screens.
