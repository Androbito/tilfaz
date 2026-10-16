# 0004. Room as the offline-first source of truth for the catalog

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub

## Context

The owner's provider exposes about 12k channels, 150k movies and 27k series. The incumbent app re-downloads
these lists often and feels slow. The provider API returns whole lists (no pagination), so one refresh is a few MB of JSON.

## Options considered

1. **Room cache, UI reads only from the database, explicit refresh per catalog**: Pros: instant start, works offline, search with SQL `LIKE`, one sync timestamp per catalog ("last updated 42 min ago"). Cons: a schema to maintain; first launch waits for a full download.
2. **In-memory cache per process**: Pros: simplest. Cons: cold start downloads everything again, which is the problem being solved.
3. **Fetch per category on demand**: Pros: smaller requests. Cons: the Xtream API does not filter server-side reliably; search across categories becomes impossible offline.

## Decision

We will store categories, channels, movies and series in Room, have every screen observe the database, and refresh a catalog
only on user request or when it is empty (`SyncCatalogUseCase`). Details (plot, episodes) are fetched on open and cached in memory.

## Consequences

- Positive: lists open in milliseconds, favourites/history/progress join against local tables.
- Negative: a stale catalog until the user refreshes; schema changes wipe the cache (destructive migration is acceptable for a cache).
- Follow-up: background refresh with WorkManager once a day on Wi-Fi; FTS table if `LIKE` search gets slow on 150k rows.
