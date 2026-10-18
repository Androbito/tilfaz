# What only you can do to publish Tilfaz

The code, the release workflow and the documentation are done (see `docs/release.md` and ADR 0010). These steps need your
accounts, your passwords or your decision. Each stops until you have done it. Nothing here is public until you say so.

## 1. Create the upload keystore

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

- [ ] Create the app: name Tilfaz, default language, "App", "Free".
- [ ] Add the **Android TV** form factor and answer its questions (D-pad only, banner, TV screenshots).
- [ ] Declarations: ads (none), content rating questionnaire, target audience (not for children), data safety.
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

- TV screenshots 1920x1080 of the real app, taken on the emulator against the mock server so that no provider channel or logo appears.
- Demo credentials for the reviewers: a mock Xtream server with legal sample streams, hosted publicly, or a provider test account.
- To change the logo: draw it on a flat green background, cut it out with `java tools/brand/KeyGreen.java <green image> design/brand/logo.png`, then rebuild every image with `java tools/brand/MakeBrandAssets.java . <Roboto-Bold.ttf>` (Roboto Bold is the font of the name on the feature graphic).
