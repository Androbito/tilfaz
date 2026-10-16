---
name: mobile-release-pipeline
description: Set up or improve CI/CD and release engineering for Android and iOS apps: pull request checks, automated signing and store uploads with Fastlane, trunk-based development, feature flags and remote config, staged rollouts, crash monitoring and forced-update kill switches. Use this skill whenever the user mentions CI, GitHub Actions, Bitrise, Xcode Cloud, Fastlane, Play Console or TestFlight uploads, release process, versioning, feature flags, staged or phased rollout, crash-free rate, or "how do we ship safely".
---

# Mobile Release Pipeline

A shipped mobile build can't be rolled back on users' phones. The pipeline must catch problems before release and limit blast radius after.

## Target pipeline

```
PR opened        -> lint + static analysis + unit tests + build (debug)        [every PR]
merge to main    -> full tests + screenshot tests + build release + internal distribution
release tag      -> signed release build -> store upload (internal / TestFlight)
store            -> staged rollout 1% -> 10% -> 50% -> 100%, halt on crash-free < threshold
```

## Workflow

1. **Inspect** existing CI files (`.github/workflows`, `bitrise.yml`, `ci_scripts/`), `fastlane/`, Gradle tasks, Xcode schemes, how versions are set.
2. **Propose the minimum next step** that gives the most safety, usually PR checks first, then automated distribution, then store automation.
3. **Generate config** from `templates/`. Adapt module names, schemes, JDK/Xcode versions, and secrets names. Never write real secrets into files; reference CI secrets.
4. **Document secrets** the user must create (keystore base64, key alias/passwords, App Store Connect API key, Play service account JSON) in the PR description or a `docs/release.md`.
5. **Add safety nets**: feature flags for unfinished work, staged rollout config, crash reporting checks, a forced-update mechanism.

## Practices

| Practice | Guidance |
| --- | --- |
| Trunk-based development | Short-lived branches (< 2 days), merge behind flags, no long release branches unless store review demands a hotfix branch |
| Versioning | Semantic version for `versionName` / `CFBundleShortVersionString`; monotonically increasing build number from CI run number |
| Changelog | Conventional Commits + release-please or git-cliff to generate notes |
| Feature flags | Remote config (Firebase Remote Config, LaunchDarkly, Unleash, ConfigCat). Wrap flags behind a domain interface (`FeatureFlags.isEnabled(Feature.NEW_CHECKOUT)`) so the provider is swappable and tests use a fake. Remove flags after full rollout |
| Staged rollout | Play: `userFraction` in Fastlane `supply`; iOS: phased release (7-day automatic), pause if needed |
| Monitoring | Crashlytics or Sentry; alert when crash-free users < 99.5% or ANR rate > 0.47% (Play bad behaviour threshold) |
| Forced update | Remote minimum supported version + Play In-App Updates API / custom App Store prompt |
| Reproducibility | Pin Gradle wrapper, JDK, Xcode version, Ruby/Fastlane via Gemfile.lock |
| Speed | Gradle configuration and build cache, SPM/derived data cache, run only affected modules where possible |

## Templates

- `templates/android-ci.yml`: GitHub Actions PR checks for Android
- `templates/ios-ci.yml`: GitHub Actions PR checks for iOS
- `templates/Fastfile`: lanes for internal distribution and store upload on both platforms

Copy, then adjust. Explain each secret the user needs to add.
