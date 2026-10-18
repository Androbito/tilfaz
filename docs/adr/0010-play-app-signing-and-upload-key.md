# 0010. Play App Signing with an upload key in GitHub secrets

- Status: Accepted
- Date: 2026-10-18
- Deciders: Ayoub

## Context

Tilfaz is published on Google Play from a single developer's account. A new app must be uploaded as an Android App Bundle and
signed through Play App Signing. Whatever key signs the bundle must never be in the repository, and a release must not depend on
one laptop. The project has GitHub Actions for pull requests and nothing for releases.

## Options considered

1. **Keep the app signing key ourselves.** Pros: full control. Cons: losing the key ends updates for good; Play does not accept it for a new app published as a bundle.
2. **Play App Signing, upload key in GitHub secrets, a backup copy outside the repository.** Pros: Google holds the key that users' devices trust; a lost or leaked upload key is replaced by asking Google to reset it. Cons: a few secrets to maintain.
3. **Play App Signing, upload key in a cloud KMS.** Pros: the key never leaves the HSM. Cons: an extra service and cost for one developer and one app.

For the upload step:

1. **`r0adkll/upload-google-play`.** Pros: one workflow step, no Ruby or Bundler, uploads the R8 mapping file. Cons: a third-party action, so it is pinned to a commit.
2. **Fastlane `supply`.** Pros: also manages listing text and screenshots, and has a promote lane. Cons: needs Ruby, a Gemfile and a Fastfile for one upload call.

## Decision

We will use Play App Signing with an upload key held in GitHub secrets and in a backup outside the repository, and upload with
`r0adkll/upload-google-play` pinned to a commit, because it is the smallest setup that keeps every key out of the project.

## Consequences

- Positive: no keystore, password or service account key is ever in the repository; the build reads the upload key only from `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` (environment or Gradle properties). With none set, a release build is still signed with the debug key, so local and pull request builds keep working; with only some set, the build fails.
- Positive: the release run refuses to upload a bundle signed with the debug key.
- Negative: the first bundle has to be uploaded by hand in the Play Console, which is what activates Play App Signing. The service account then needs release rights on the app.
- Negative: that first bundle uses `versionCode` 1, so CI adds an offset of 100 to the run number (`versionCode = run number + 100`). The offset must not be lowered.
- Negative: the workflow sends to internal testing only; promotion to production stays a manual decision in the Play Console.
- Follow-up: move to Fastlane `supply` if listing text and screenshots should come from the repository; protect the release job with a GitHub Environment that needs approval if more people get push access.
