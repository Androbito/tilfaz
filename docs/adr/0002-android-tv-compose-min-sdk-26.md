# 0002. Android TV only, Compose for TV, minSdk 26

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub

## Context

The first version replaces a single IPTV app on the owner's TV. The reference UI (IPTV Smarters Pro) is a 10-foot,
D-pad-driven layout. Android TV boxes in the wild mostly run Android 9 to 12.

## Options considered

1. **Android TV only with Jetpack Compose for TV (`androidx.tv:tv-material`)**: Pros: one UI surface, modern toolkit, focus handling built in. Cons: tv-material is younger than phone Material 3; no text field component.
2. **Android TV + phone/tablet with a shared data layer**: Pros: wider reach. Cons: roughly doubles UI work for a v1 nobody asked for.
3. **Leanback (View-based) library**: Pros: battle-tested. Cons: legacy, slow iteration, poor fit with the rest of the stack.

## Decision

We will target Android TV only, with Compose for TV, and minSdk 26 (Android 8.0) so `java.time` is available without desugaring.

## Consequences

- Positive: a single, focus-first UI; `:core:model`/`:core:domain` can use `java.time` directly and stay JVM-only.
- Negative: a phone app later means a second set of screens (the domain and data layers are reusable as-is).
- Follow-up: revisit minSdk if a target box runs Android 7 (then add core library desugaring).
