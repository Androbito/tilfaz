# Audit checklists

Mark each item Pass / Fail / N/A with evidence.

## Offline-first and data

- [ ] UI observes local storage (Room Flow, SwiftData/Core Data observation), not raw network responses
- [ ] Network refresh writes into local storage
- [ ] Mutations made offline are queued and retried (WorkManager with constraints, BGTaskScheduler / background URLSession)
- [ ] Clear UX for stale data and sync errors (timestamp, retry action)
- [ ] Database migrations exist and are tested; no `fallbackToDestructiveMigration` in production
- [ ] Pagination for large lists (Paging 3, cursor-based APIs)

## Lifecycle and state

- [ ] Critical UI state survives process death (SavedStateHandle, `rememberSaveable`, `@SceneStorage`)
- [ ] Flows collected with `collectAsStateWithLifecycle` / `repeatOnLifecycle`
- [ ] No Activity/Fragment/View/Context stored in ViewModels or singletons
- [ ] iOS: no retain cycles in closures (`[weak self]` where the closure outlives the call), Tasks cancelled on disappear
- [ ] Configuration changes (rotation, dark mode, font size) don't lose input

## Concurrency

- [ ] No network, disk or database work on the main thread (StrictMode in debug builds)
- [ ] No `GlobalScope`; scopes tied to lifecycle (`viewModelScope`, `.task {}`)
- [ ] Dispatchers injected, not hardcoded
- [ ] Swift: `@MainActor` on UI types, `Sendable` conformance, strict concurrency checks enabled or warnings tracked
- [ ] Cancellation handled (no work continuing after the user leaves)

## Performance

- [ ] Cold start measured (Macrobenchmark, `Time to initial display`, MetricKit / Xcode Organizer)
- [ ] Baseline Profiles generated (Android)
- [ ] Lazy lists use stable keys; no expensive calculation in composition or `body`
- [ ] Images loaded with a caching library (Coil, Glide, Kingfisher, Nuke), downsampled to view size
- [ ] Heavy initialisation deferred (App Startup, lazy DI)
- [ ] App size monitored; unused resources removed (`shrinkResources`), asset catalogs used

## Battery and network

- [ ] No polling loops; push notifications or server-sent updates where needed
- [ ] Requests batched; HTTP cache configured
- [ ] Background work uses OS schedulers with constraints (unmetered, charging when possible)
- [ ] Location accuracy and frequency minimal for the feature

## Security (OWASP MASVS v2)

- [ ] MASVS-STORAGE: tokens and secrets in EncryptedSharedPreferences/DataStore + Keystore, Keychain on iOS; no secrets in plain prefs/UserDefaults
- [ ] MASVS-CRYPTO: platform crypto APIs, no custom algorithms, no hardcoded keys
- [ ] MASVS-AUTH: tokens refreshed securely, biometric prompts bound to a crypto operation when protecting secrets
- [ ] MASVS-NETWORK: HTTPS only (`usesCleartextTraffic=false`, ATS enabled), certificate pinning for sensitive apps with a rotation plan
- [ ] MASVS-PLATFORM: exported components minimal, deep links validated, WebView JS bridges restricted, no `allowFileAccess` misuse
- [ ] MASVS-CODE: R8/ProGuard enabled for release, dependencies up to date, no debug flags in release
- [ ] MASVS-RESILIENCE (if needed): root/jailbreak and tamper detection, Play Integrity / App Attest
- [ ] MASVS-PRIVACY: no PII in logs or crash reports, analytics IDs resettable
- [ ] No API keys or credentials committed to the repo or embedded in the binary

## Accessibility

- [ ] Every interactive element has a label (`contentDescription`, `semantics`, `accessibilityLabel`)
- [ ] Decorative images hidden from assistive tech
- [ ] Touch targets at least 48x48 dp / 44x44 pt
- [ ] Text contrast at least 4.5:1 (3:1 for large text)
- [ ] Layouts survive font scale 200% / largest Dynamic Type without truncation of essential info
- [ ] Logical focus order; headings marked (`heading()`, `.accessibilityAddTraits(.isHeader)`)
- [ ] State conveyed by more than colour
- [ ] Tested with TalkBack and VoiceOver on at least the main journeys

## Localisation and RTL

- [ ] All user-facing strings in resources (`strings.xml`, String Catalogs)
- [ ] Plurals use plural resources / stringsdict
- [ ] Dates, numbers, currency formatted with locale-aware formatters
- [ ] Android: `supportsRtl="true"`, start/end instead of left/right; iOS: leading/trailing
- [ ] Directional icons mirrored (`autoMirrored`, `.flipsForRightToLeftLayoutDirection`)
- [ ] Pseudo-locales (`en-XA`, `ar-XB`) or RTL preview checked

## Privacy and store compliance

- [ ] Permissions requested in context with rationale, minimal set
- [ ] iOS privacy manifest declares required-reason APIs and tracking domains
- [ ] App Tracking Transparency prompt before any cross-app tracking
- [ ] Play Data Safety form matches actual collection
- [ ] GDPR: consent collected before analytics/ads SDKs initialise (EU users)
- [ ] Account deletion available in-app if accounts can be created (store requirement)

## Design system

- [ ] Colours, typography, spacing, radius from tokens; no hardcoded hex or raw dp/pt in feature modules
- [ ] Light and dark themes both work
- [ ] Shared components reused instead of re-implemented per screen

## Compatibility and release safety

- [ ] Min SDK / deployment target documented and justified
- [ ] New APIs guarded (`Build.VERSION.SDK_INT`, `#available`)
- [ ] Remote kill switch / forced update mechanism exists
- [ ] Crash reporting and ANR monitoring wired in release builds
