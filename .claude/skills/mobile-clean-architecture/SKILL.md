---
name: mobile-clean-architecture
description: Design, scaffold, or refactor native mobile apps (Android Kotlin/Compose, iOS Swift/SwiftUI, Kotlin Multiplatform) around Clean Architecture, MVVM/MVI, Unidirectional Data Flow and feature modularization. Use this skill whenever the user starts a new mobile app or feature, asks "how should I structure this", wants to add a screen, a use case, a repository or a module, mentions layers, domain, data layer, ViewModel, UiState, or wants an existing codebase reorganised, even if they never say "clean architecture".
---

# Mobile Clean Architecture

Structure native apps so that business rules are independent of the UI, the network and the database. The goal is code that is testable in milliseconds, easy to change, and easy for a team to work on in parallel. Apply it pragmatically: the architecture serves the product, not the other way round.

## The layers

| Layer | Owns | Must NOT import |
| --- | --- | --- |
| Presentation | Screens, UI state, user events, navigation | Retrofit, Room, URLSession, SwiftData, DTOs |
| Domain | Entities, use cases, repository interfaces, business rules | Android SDK, UIKit/SwiftUI, any framework or I/O library |
| Data | Repository implementations, API clients, DTOs, local storage, mappers | Presentation code |

Dependency rule: presentation -> domain <- data. The domain declares interfaces (`OrderRepository`), the data layer implements them, DI wires them. Nothing points outward from the domain.

The domain layer is optional for thin features. Don't create a use case that only forwards one repository call. Add it when there is real logic (validation, combining sources, business rules) or reuse across screens.

## Workflow

1. **Detect the platform and current state.** Look for `build.gradle(.kts)`, `settings.gradle`, `Package.swift`, `.xcodeproj`. Read the existing package/module layout before proposing anything. Follow existing conventions when they are reasonable.
2. **Model the feature from the domain inward-out.** Start with entities and the repository interface in plain Kotlin/Swift. Then the use case(s) if justified. Then data implementation. Then the ViewModel and UI last.
3. **Define one immutable UI state per screen** plus a sealed set of user events/intents (UDF). The view renders state and forwards events, nothing else.
4. **Map at the boundaries.** DTO -> domain model in the data layer, domain model -> UI model in the presentation layer when the screen needs formatting. Never leak DTOs or DB entities into the UI.
5. **Errors as values.** Return `Result`/sealed outcomes from repositories (Kotlin `Result` or a sealed `DomainResult`, Swift `Result` or `throws` with typed domain errors). Map technical errors (HTTP 500, IOException, URLError) to domain errors in the data layer.
6. **Wire with DI.** Hilt or Koin on Android; initializer injection plus a light container on iOS. Every collaborator behind an interface/protocol so tests can use fakes.
7. **Write the tests alongside** (see the `mobile-testing-strategy` and `tdd-craftsmanship` skills if installed): use case and ViewModel unit tests with fakes are the minimum.

## Modularization

Recommend it once the app has more than a handful of features or more than one or two developers.

```
:app                      // composition root, navigation graph, DI setup
:core:model               // shared domain entities (pure)
:core:domain              // shared use cases (optional)
:core:data                // repositories shared across features
:core:network             // HTTP client, interceptors, auth
:core:database            // local persistence
:core:designsystem        // theme, tokens, reusable components
:core:testing             // fakes, test rules, fixtures
:feature:<name>           // one per feature: ui + viewmodel (+ feature-local domain/data)
```

Rules: features never depend on other features; they depend only on `:core:*`. Navigation between features goes through routes defined in `:app` or a small `:core:navigation` module. On iOS, mirror this with local Swift packages (`Core/Network`, `Features/Checkout`), one target per module.

## Platform templates

Read the reference for the platform you are working on before generating code:

- Android (Kotlin, Compose, Hilt, Coroutines/Flow): `references/android.md`
- iOS (Swift, SwiftUI, async/await, Observation): `references/ios.md`
- Kotlin Multiplatform (shared domain/data, native or Compose Multiplatform UI): `references/kmp.md`. Detect KMP by a `shared/` module, `kotlin("multiplatform")` plugin or `commonMain` source sets.

## Review checklist

Before declaring a feature done, check:

- Domain files compile without any framework import.
- The ViewModel exposes exactly one state stream and receives events; no public mutable state.
- No business logic in Composables / Views (formatting helpers are fine).
- No DTO, `@Entity`, `Codable` network model or `NSManagedObject` visible to the UI.
- Repositories return domain models and domain errors.
- Each new class has a single reason to change.
- There is at least one unit test for the ViewModel and one per use case.

## Anti-patterns to call out

- "Manager"/"Helper"/"Util" god classes that mix networking, caching and formatting.
- Use cases that only forward a call (`GetUserUseCase` that returns `repo.getUser()`).
- A `BaseViewModel` / `BaseFragment` inheritance chain carrying unrelated concerns.
- Feature modules importing each other.
- Singletons accessed statically instead of injected.
- One giant `UiState` shared by several screens.
