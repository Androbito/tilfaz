# 0003. Multi-module Clean Architecture with convention plugins

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub

## Context

The app has eight distinct features and three infrastructure concerns (network, database, settings). The
`mobile-clean-architecture` skill recommends modularization past a handful of features. Build consistency across
15 modules matters more than in a single-module app.

## Options considered

1. **Multi-module (`:core:*`, `:feature:*`) with Gradle convention plugins in `build-logic/`**: Pros: enforced layering (domain is a JVM module and physically cannot import Android), parallel builds, one place to configure Kotlin/Compose/Hilt. Cons: more Gradle files up front.
2. **Single module with packages**: Pros: trivial build. Cons: the dependency rule is a convention only; the first `import android.*` in the domain goes unnoticed.
3. **Multi-module without convention plugins**: Pros: fewer concepts. Cons: 15 copies of the same `android {}` block drift apart.

## Decision

We will use a multi-module layout with convention plugins (`tilfaz.android.application|library|library.compose|feature|hilt|room`, `tilfaz.jvm.library`),
features depending only on `:core:*`, and navigation wired in `:app`.

## Consequences

- Positive: `:core:model` and `:core:domain` compile with `kotlin("jvm")`, so their tests run in milliseconds and cannot touch the SDK.
- Negative: adding a feature means a new module and a line in `settings.gradle.kts` and `TilfazNavHost`.
- Follow-up: generate a baseline profile once the app is stable.
