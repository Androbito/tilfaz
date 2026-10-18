# What only you can do to publish Tilfaz

The code, the release workflow and the documentation are done (see `docs/release.md` and ADR 0010). These steps need your
accounts, your passwords or your decision. Each stops until you have done it. Nothing here is public until you say so.

## 1. Create the upload keystore (done: the key is managed by EAS)

Done on 2026-10-18: `~/secrets/tilfaz-upload.jks` and `tilfaz-upload.properties` exist, and EAS (`@androbito/tilfaz`, "Tilfaz upload key") holds the same key. Certificate SHA-256 `3F:C0:2F:80:68:9E:53:CA:EB:09:8E:AA:D8:FA:14:F4:FF:71:09:D5:3A:8B:4F:06:42:56:80:7F:E1:B9:B5:43`. Never regenerate it. The first bundle, `~/secrets/release/tilfaz-0.1.0-vc1.aab` (versionCode 1), is built, signed with that key and verified. The command below is kept for the record only.

On your machine, in a terminal. You choose the passwords; `keytool` asks for them. Save them in your password manager.

```bash
mkdir -p ~/secrets && chmod 700 ~/secrets
keytool -genkeypair -v -keystore ~/secrets/tilfaz-upload.jks -alias tilfaz-upload \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Tilfaz Upload, O=Androbito"
```

Then copy `~/secrets/tilfaz-upload.jks` to an encrypted drive. The file stays out of the project.

## 2. Create the GitHub secrets

Names and commands are in `docs/release.md`, section "The GitHub secrets":
`UPLOAD_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`.
The first four can be set now; the last one comes from step 4.

## 3. Play Console: the app and its first bundle

Progress (2026-10-18): the app **Tilfaz** is created (app id `4975691540799348501`, `https://play.google.com/console/u/0/developers/7880441050556061813/app/4975691540799348501/app-dashboard`), free, default language en-US, both declarations accepted. The Android TV form factor is added; still locked behind it: import Android TV screenshots, then activate distribution on Android TV and accept the review rules. Then the content declarations, store listing and the first release.

- [x] Create the app: name Tilfaz, default language, "App", "Free".
- [ ] Add the **Android TV** form factor and answer its questions (D-pad only, banner, TV screenshots).
- [x] Declarations: all saved (ads, health, financial, government, content rating, data safety, app access with reviewer account, target audience 18+, category, contact e-mail).
- [ ] Privacy policy URL: `https://androbito.github.io/tilfaz-privacy/` (published, French and English; its source is `store/privacy-policy/index.html`, push changes to the `tilfaz-privacy` repository).
- [ ] App access: the app needs a login to a provider, so give the reviewers demo credentials (see "Still open" below).
- [ ] Store listing: icon 512x512, feature graphic 1024x500 and TV banner 1280x720 (all three in `store/`), TV screenshots 1920x1080, short and full description.
- [ ] Build the first bundle with the upload key and `versionCode` 1 (command in `docs/release.md`) and **upload it by hand** to the
  internal testing track. Accept Play App Signing when asked. This is what registers your upload certificate with Google.

## 4. Play Console and Google Cloud: the service account for CI

- [ ] In Google Cloud: create a project (or use one), enable the Google Play Android Developer API, create a service account and a
  JSON key for it. Save the key as `~/secrets/play-service-account.json`.
- [ ] In the Play Console, Users and permissions: invite the service account's e-mail address and give it, on Tilfaz only, the right to
  release to testing tracks (and to view app information).
- [ ] Set the secret: `gh secret set PLAY_SERVICE_ACCOUNT_JSON --repo Androbito/tilfaz < ~/secrets/play-service-account.json`

## 5. First automated release

```bash
git tag v0.1.1 && git push origin v0.1.1
```

Watch the "Android Release" run on GitHub. It should end with the bundle on the internal testing track as version code 101
(run number + 100). If it stops on a missing secret, the error names it.

## 6. Production

Never from CI and never without your explicit yes. A personal developer account may first need a closed test with 12 testers for
14 days before the Play Console lets you apply for production; check what your Console shows. When you decide to go public, promote
the tested build yourself in the Play Console, with a staged rollout.

## Still open before the listing is complete

- TV screenshots: done, `store/screenshots/1-home.png` to `4-series.png` (1920x1080, drawn from the real screens with invented data, no provider content). Rebuild with `STORE_SCREENSHOTS=1 ./gradlew :app:testDebugUnitTest --tests '*StoreScreenshots*'`.
- Demo credentials for the reviewers: a mock Xtream server with legal sample streams, hosted publicly, or a provider test account.
- To change the logo: draw it on a flat green background, cut it out with `java tools/brand/KeyGreen.java <green image> design/brand/logo.png`, then rebuild every image with `java tools/brand/MakeBrandAssets.java . <Roboto-Bold.ttf>` (Roboto Bold is the font of the name on the feature graphic).
