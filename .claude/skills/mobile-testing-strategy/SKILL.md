---
name: mobile-testing-strategy
description: Plan and write tests for native mobile apps following the test pyramid: unit tests for use cases and ViewModels, integration tests for repositories and persistence, screenshot/snapshot tests for UI components, and a thin layer of end-to-end UI tests. Use this skill whenever the user asks to add tests, improve coverage, test a ViewModel, a repository, a Composable or a SwiftUI view, set up Paparazzi/Roborazzi/snapshot testing, Maestro, XCUITest or Espresso, deal with flaky tests, or asks "what should I test" in an Android or iOS project.
---

# Mobile Testing Strategy

Fast, deterministic tests at the bottom; a few slow, high-value journey tests at the top.

| Level | Targets | Android | iOS | Volume |
| --- | --- | --- | --- | --- |
| Unit | Use cases, ViewModels, mappers, reducers, validators | JUnit, kotlinx-coroutines-test, Turbine | Swift Testing, XCTest | Most |
| Integration | Repository + real local DB, API client + mock server, serialization | Room in-memory, MockWebServer | In-memory SwiftData/Core Data, URLProtocol stubs | Some |
| Screenshot | Design-system components and screen states | Paparazzi or Roborazzi | swift-snapshot-testing | Per component and state |
| UI / E2E | Critical journeys: login, onboarding, checkout | Compose UI Test, Maestro | XCUITest, Maestro | Few |

## Workflow

1. **Inventory**: list test source sets/targets, frameworks already in use, existing fakes. Reuse them.
2. **Decide what to test** for the code in scope with the matrix below. Prioritise by risk: money, auth, data loss, crashes.
3. **Create or reuse fakes** in a shared test module (`:core:testing`, `TestSupport` target).
4. **Write tests** using the patterns in `references/patterns.md`.
5. **Run them** with the project's commands (`./gradlew testDebugUnitTest`, `xcodebuild test -scheme ...` or `swift test`) and fix failures before reporting.
6. **Report** what is covered and the remaining gaps in priority order.

## What to test per component

| Component | Must test | Skip |
| --- | --- | --- |
| Use case | Every business rule branch, error mapping | Pure delegation |
| ViewModel | Initial state, each event's resulting state, loading and error states, one-off effects | Framework plumbing |
| Repository | Mapping DTO -> domain, cache/refresh policy, error translation | The HTTP library itself |
| Mapper / formatter | Edge cases: nulls, empty, locale, currency, RTL strings | Trivial field copies |
| Stateless screen | Snapshot of each meaningful state (loading, empty, content, error), dark mode, large font, RTL | Pixel tests of every screen variant |
| Journey | Happy path of revenue-critical flows | Everything else |

## Determinism rules

- Inject dispatchers / executors; use `StandardTestDispatcher` or `UnconfinedTestDispatcher` deliberately; in Swift, avoid real sleeps, inject a `Clock`.
- Inject `Clock`, UUID and random providers.
- Never hit real networks in unit or integration tests; use the `api-mock-server` skill or MockWebServer / URLProtocol.
- Fix locale and time zone in tests that format dates or money.
- A flaky test is a bug: find the source of nondeterminism, don't add retries.

## Coverage

Report coverage (Kover / JaCoCo, Xcode coverage) but don't chase a number. Aim for near-full coverage of domain and ViewModels; UI and glue code can be lower. A useful CI gate is "coverage of changed lines" rather than global percentage.

See `references/patterns.md` for code templates.
