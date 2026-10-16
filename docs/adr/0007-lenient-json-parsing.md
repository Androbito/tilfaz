# 0007. Lenient DTO parsing for Xtream panels

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub

## Context

Xtream Codes "API" responses come from PHP panels with no schema: ids as `12` or `"12"`, ratings as `"7,5"`, empty strings
for nulls, `exp_date` as `"null"`, `backdrop_path` as a string or an array, `episodes` keyed by season or as nested arrays.
A strict parser fails on the first provider that differs, which is exactly how "reading error"-type bugs are born.

## Options considered

1. **kotlinx.serialization with per-field lenient serializers (`LenientInt`, `LenientString`, `EpisodesBySeason`...)**: Pros: never fails on a type mismatch, each quirk gets a unit test, DTOs stay declarative. Cons: every DTO field needs the annotation.
2. **Parse everything as `JsonElement` and map by hand**: Pros: total control. Cons: verbose, no compile-time shape.
3. **Moshi/Gson with custom adapters**: Pros: familiar. Cons: no advantage over kotlinx for a Kotlin-only codebase.

## Decision

We will use kotlinx.serialization with lenient field serializers and `ignoreUnknownKeys`, and drop rows that lack an id rather than inventing one.

## Consequences

- Positive: a new provider quirk is a one-line serializer tweak plus a test, never an app crash.
- Negative: slightly noisier DTO declarations.
- Follow-up: record a real panel's responses with the mock server's `--record` flag and add them as fixtures.
