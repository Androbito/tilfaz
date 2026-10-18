# Releasing Tilfaz

A tag `vMAJOR.MINOR.PATCH` builds a signed Android App Bundle in GitHub Actions and sends it to the Play **internal testing**
track. Promoting it to production is always a manual step in the Play Console. The decisions behind this are in
[ADR 0010](adr/0010-play-app-signing-and-upload-key.md).

## What exists

- `.github/workflows/android-ci.yml`: ktlint, lint, unit tests, screenshot tests, debug APK and coverage on every pull request and on main.
- `.github/workflows/android-release.yml`: on a `v*` tag, the same checks, then the signed bundle, then the upload to internal testing.
- `app/build.gradle.kts`: the release build is signed with the upload key when `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` are all set (environment or `-P` properties). With none set it is signed with the debug key, which is what local builds do. With only some set it fails.

No keystore, password or service account key is ever committed. `.gitignore` covers `*.jks`, `*.keystore`, `keystore.properties`, `play-service-account*.json` and `secrets/`.

## One-time setup

### 1. The upload keystore

Created once, by hand, on your machine. The files live in `~/secrets/`, never in the project:

```bash
mkdir -p ~/secrets && chmod 700 ~/secrets
keytool -genkeypair -v -keystore ~/secrets/tilfaz-upload.jks -alias tilfaz-upload \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Tilfaz Upload, O=Androbito"
```

`keytool` asks for the password twice. Choose a long one, and save it in your password manager together with the alias
(`tilfaz-upload`). Keep a second copy of the `.jks` on an encrypted drive. This is the *upload* key: if it is lost or leaked, Google
can replace it (see below), so it is not the end of the app.

### 2. The GitHub secrets

Names the release workflow expects (repository settings, or `gh`):

| Secret | What it is |
| --- | --- |
| `UPLOAD_KEYSTORE_BASE64` | the `.jks` file, base64 encoded |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `tilfaz-upload` |
| `KEY_PASSWORD` | the key password (the same value as the keystore password for a PKCS12 keystore) |
| `PLAY_SERVICE_ACCOUNT_JSON` | the whole JSON key of the Google Cloud service account |

```bash
base64 -i ~/secrets/tilfaz-upload.jks | gh secret set UPLOAD_KEYSTORE_BASE64 --repo Androbito/tilfaz
gh secret set KEYSTORE_PASSWORD --repo Androbito/tilfaz        # paste when asked
gh secret set KEY_ALIAS --repo Androbito/tilfaz
gh secret set KEY_PASSWORD --repo Androbito/tilfaz
gh secret set PLAY_SERVICE_ACCOUNT_JSON --repo Androbito/tilfaz < ~/secrets/play-service-account.json
```

### 3. The Play Console

Done by hand, once: create the app, answer the forms, upload the **first bundle yourself** (this switches on Play App Signing and
registers your upload certificate), then create a Google Cloud service account, invite it in the Play Console with the right to
release to testing tracks, and keep its JSON key in `~/secrets/`. The exact list is in `HUMAN_TODO.md`.

For that first bundle, build it locally with the upload key and `versionCode` 1. The passwords are typed at a prompt so they stay
out of the shell history:

```bash
read -rs KEYSTORE_PASSWORD && export KEYSTORE_PASSWORD KEY_PASSWORD="$KEYSTORE_PASSWORD"
export KEYSTORE_PATH=~/secrets/tilfaz-upload.jks KEY_ALIAS=tilfaz-upload
./gradlew :app:bundleRelease -PversionCode=1 -PversionName=0.1.0
unset KEYSTORE_PASSWORD KEY_PASSWORD
```

The bundle is `app/build/outputs/bundle/release/app-release.aab`.

## Releasing

```bash
git tag v0.1.1 && git push origin v0.1.1
```

`versionName` is the tag without the `v`. `versionCode` is the run number of the release workflow plus 100, so it always rises and
stays above the hand-uploaded first bundle. Do not lower that offset.

## The direct-download APK

Outside Play, the signed APK is a GitHub release (`apk-v0.1.0` for the first one). Build it with the same four signing variables as
the bundle: `./gradlew :app:assembleRelease -PversionCode=N -PversionName=X`, then check it with `apksigner verify --print-certs`
(the certificate must be the upload key's, not the debug one). Attach it twice: as `tilfaz-X.apk` and as `tilfaz.apk`, plus a `.sha256`.
The unversioned name keeps `https://github.com/Androbito/tilfaz/releases/latest/download/tilfaz.apk` valid, and the AFTVnews Downloader
code **2086257** points at that link, so it never has to change. The APK shares the upload key's signature, not Play's, so a device
that has one cannot update from the other.

## Checking a build

```bash
AAB=app/build/outputs/bundle/release/app-release.aab
keytool -printcert -jarfile "$AAB"          # CN=Android Debug means the upload key was not applied
./gradlew :app:assembleRelease && ~/Library/Android/sdk/build-tools/37.0.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk
./gradlew ktlintCheck lintDebug testDebugUnitTest
```

The release APK carries the same manifest as the bundle. In the `badging` output expect `leanback-launchable-activity`, a `banner=`
on the application, `uses-feature: name='android.software.leanback'` and
`uses-feature-not-required: name='android.hardware.touchscreen'`.

## If the upload key is lost or leaked

Play App Signing holds the key that users' devices trust, so the app is not lost. Create a new keystore as above, export its
certificate:

```bash
keytool -export -rfc -keystore ~/secrets/tilfaz-upload-new.jks -alias tilfaz-upload -file ~/secrets/upload_certificate.pem
```

then in the Play Console, App integrity, App signing, **Request upload key reset**, and upload that PEM. Google confirms by mail;
afterwards update `UPLOAD_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. A leaked service account key is
revoked in Google Cloud and replaced; update `PLAY_SERVICE_ACCOUNT_JSON`.

## Still to add

- Crash reporting (Firebase Crashlytics or Sentry) behind a `CrashReporter` interface so debug builds use a no-op.
- A forced update: a remote minimum version checked at launch, with a blocking screen below it.

## Android TV store requirements to keep in mind

- Leanback launcher intent and banner are in the manifest already; the banner must stay 320x180.
- No touchscreen requirement (`android.hardware.touchscreen required=false`).
- Every screen must be fully operable with a D-pad: PR reviews should check focus order on new screens.
