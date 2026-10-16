# 0008. Hilt for dependency injection

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub

## Context

Fifteen modules, ViewModels with four to seven collaborators each, and a requirement that every collaborator sits behind an
interface so tests use fakes. The `mobile-clean-architecture` skill allows Hilt or Koin.

## Options considered

1. **Hilt (KSP)**: Pros: compile-time validation of the graph, `@HiltViewModel` + `hiltViewModel()` with type-safe navigation, `SavedStateHandle` injection. Cons: build time cost of annotation processing.
2. **Koin**: Pros: no codegen, faster builds. Cons: graph errors at runtime; weaker IDE support for navigation-scoped ViewModels.
3. **Manual DI**: Pros: zero dependencies. Cons: 15 modules of boilerplate.

## Decision

We will use Hilt with KSP; repository bindings live in `:core:data` (`DataModule`), infrastructure providers in `:core:network` and `:core:database`.

## Consequences

- Positive: a missing binding fails the build, not the demo.
- Negative: Hilt-annotated modules must be Android libraries; the pure JVM modules only use `javax.inject`.
- Negative: Hilt 2.59+ requires AGP 9; this project stays on Hilt 2.58 until the AGP 9 migration (built-in Kotlin, plugin changes) is done deliberately. Renovate/Dependabot must not bump Hilt alone.
- Follow-up: migrate to AGP 9 + Hilt 2.60 as one change, verified by the full test suite.
