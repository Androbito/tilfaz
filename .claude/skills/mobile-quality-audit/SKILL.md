---
name: mobile-quality-audit
description: Audit an Android or iOS codebase (or a single feature/PR) for mobile-specific quality: offline-first data flow, lifecycle and process death, concurrency, performance, battery and network usage, security (OWASP MASVS), accessibility, localisation and RTL, privacy compliance, design-system consistency and backward compatibility. Use this skill whenever the user asks for an app audit, a production-readiness or pre-release check, security review, accessibility review, "is my app ready for the store", performance problems, jank, ANRs, battery drain, or adding Arabic/Hebrew/RTL support.
---

# Mobile Quality Audit

Mobile apps run on devices you don't control, on flaky networks, with limited battery, and the OS can kill them at any moment. This audit catches what generic code review misses.

## Workflow

1. **Scope.** Whole app, one feature, or one PR? Platform(s)? Release target (internal, beta, store)? Ask only if it cannot be inferred.
2. **Gather facts** from the code: manifest / Info.plist, Gradle / Package.swift dependencies, network layer, persistence, ViewModels, a sample of screens, CI config, `proguard-rules.pro`, privacy manifest (`PrivacyInfo.xcprivacy`), string resources.
3. **Run the checklists** in `references/checklists.md` for the areas in scope. Mark each item Pass / Fail / Not applicable with file:line evidence. Don't mark Pass without evidence.
4. **Prioritise** findings:
   - **P0**: security holes, data loss, crash or ANR risks, store rejection risks, legal (GDPR, privacy labels).
   - **P1**: broken offline behaviour, accessibility blockers (unlabelled controls, tiny touch targets), memory leaks, main-thread I/O.
   - **P2**: performance improvements, consistency, minor a11y and i18n issues.
5. **Fix or propose fixes.** If the user asked for fixes, apply P0 first, run the build and tests after each group. Otherwise give concrete code-level recommendations.
6. **Report** using the format below.

## Areas

| Area | Core expectations |
| --- | --- |
| Offline-first | Local store is source of truth, network syncs into it, writes queued and retried (WorkManager / BGTaskScheduler) |
| Lifecycle | Survives process death (SavedStateHandle, `@SceneStorage`), lifecycle-aware collection, no leaks of Context/ViewController |
| Concurrency | Structured concurrency, no main-thread I/O, cancellation respected, Swift strict concurrency warnings addressed |
| Performance | Measured startup, Baseline Profiles (Android), lazy lists with stable keys, image caching and downsampling, no heavy work in composition/`body` |
| Battery and data | No polling, batched requests, respects Doze/Low Power Mode, push over pull, HTTP caching |
| Security | OWASP MASVS: Keystore/Keychain for secrets, no secrets in the binary, TLS only, no PII in logs, R8 obfuscation, safe WebViews, deep link validation |
| Accessibility | Labels, 48dp / 44pt targets, contrast AA, Dynamic Type / font scale up to 200%, focus order, TalkBack/VoiceOver tested |
| Localisation | No hardcoded strings, plurals, locale-aware dates/money, RTL using start/end, mirrored directional icons |
| Privacy | Minimal permissions asked in context, ATT, Play Data Safety, Apple privacy manifest accurate, GDPR consent before tracking |
| Design system | Tokens for colour/type/spacing, no hardcoded hex or dp/pt in feature code, light and dark themes |
| Compatibility | Defined min SDK / iOS version, guarded newer APIs, forced-update kill switch |

## Report format

```
# Mobile quality audit: <app/feature> (<platform>, <date>)

## Verdict
<ready / ready with fixes / not ready> in one or two sentences.

## P0
| # | Area | Finding | Evidence | Fix |

## P1
...

## P2
...

## Passed checks
<compact list of areas that passed, with one line of evidence each>
```

Keep evidence concrete (file:line, config key, dependency version). If something cannot be verified from the code (for example real-device performance), say so and suggest how to measure it.
