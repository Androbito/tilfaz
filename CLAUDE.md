# Tilfaz (تلفاز)

Android TV IPTV client for Xtream Codes accounts. Kotlin, Jetpack Compose for TV, Media3, Room, Hilt.
Single account, offline-first catalog cache, now/next EPG per channel.

## Engineering standards

This project follows the nine `mobile-craft-skills` in `.claude/skills/`. Load the relevant one before working:

| When | Skill |
| --- | --- |
| New screen, use case, repository, module | `mobile-clean-architecture` (Android reference in its `references/android.md`) |
| Writing any production code | `tdd-craftsmanship`: test list first, red/green/refactor, small commits |
| Adding or fixing tests | `mobile-testing-strategy`: fakes in `:core:testing`, Turbine for flows, Roborazzi for components |
| Reviewing a PR or your own diff | `solid-code-review` |
| Before a release or when touching network, storage, player | `mobile-quality-audit` |
| CI, signing, Play upload, flags | `mobile-release-pipeline` |
| Implementing a screen from a mockup or screenshot | `figma-to-native-ui` (use the design system tokens, never raw hex/dp) |
| Needing provider data without a provider | `api-mock-server` (`mocks/README.md`) |
| Any technical choice worth remembering | `adr-writer` -> `docs/adr/` |

## Architecture in one minute

```
:app                 composition root, TilfazNavHost, Hilt, Coil image loader
:core:model          pure Kotlin entities (Account, LiveChannel, Movie, Series, Episode, Playable, StreamSource...)
:core:domain         repository interfaces + use cases (StreamUrlBuilder, ServerUrlNormalizer, NowNextResolver, CatalogBrowser, TitleSearch, SyncCatalogUseCase)
:core:network        Retrofit XtreamApi, lenient DTO serializers, request interceptor (host + credentials), safeApiCall error mapping
:core:database       Room: catalog cache + favourites, watch progress, recent channels, sync status
:core:data           repository implementations, mappers, DataStore (account encrypted via Keystore, playback settings), Hilt bindings
:core:designsystem   TilfazTheme, tokens (TilfazColors, Dimens), components (HeroTile, PosterCard, ChannelRow, CategoryRow, BrowserLayout...)
:core:testing        fakes, fixtures, MainDispatcherRule
:feature:*           onboarding (sign in), home, live, vod, series, player, settings, library (favourites, continue watching)
```

Rules that are not negotiable:

- Dependency rule: presentation -> domain <- data. `:core:model` and `:core:domain` are JVM modules: they cannot import Android.
- Features never depend on other features. Cross-feature navigation goes through callbacks wired in `TilfazNavHost`.
- One immutable `UiState` per screen, a sealed `Event` type, a stateless `Screen` composable separate from the Hilt `Route`.
- No DTO or Room entity reaches a ViewModel. Mappers live in `:core:data/mapper`.
- Errors are values: `DomainError` / `DomainException`, mapped in `safeApiCall`. Never catch `Throwable` in a ViewModel.
- Credentials never appear in logs, routes or toasts. Stream URLs are built at play time by `StreamUrlBuilder`.
- Colours, spacing and type come from `TilfazColors`, `Dimens`, `MaterialTheme.typography`. Hardcoded values need a comment.
- D-pad first: every interactive element goes through `FocusableSurface` so focus looks the same everywhere.

## Xtream API cheat sheet

- `GET {server}/player_api.php?username&password` -> account status; `&action=get_live_categories|get_live_streams|get_vod_categories|get_vod_streams|get_vod_info&vod_id|get_series_categories|get_series|get_series_info&series_id|get_short_epg&stream_id&limit`.
- Streams: `{server}/live/{u}/{p}/{id}.ts|m3u8`, `{server}/movie/{u}/{p}/{id}.{ext}`, `{server}/series/{u}/{p}/{episodeId}.{ext}`.
- Panels return numbers as strings, empty strings for nulls, EPG titles base64-encoded, `episodes` as object or array. `LenientSerializers.kt` handles all of it; add a test there when a new quirk shows up.

## Commands

```bash
./gradlew testDebugUnitTest            # all unit tests (JVM modules run as `test`)
./gradlew :core:designsystem:verifyRoborazziDebug   # screenshot tests (recordRoborazziDebug to update)
./gradlew ktlintCheck lintDebug        # static checks
./gradlew koverHtmlReport              # coverage, build/reports/kover/html
./gradlew :app:installDebug            # on a connected Android TV / emulator
node tools/mock-server/mock-server.mjs --dir mocks --port 3001   # fake Xtream panel (see mocks/README.md)
```

## Backlog (not in v1, in rough priority)

1. Multiple accounts / playlists.
2. FFmpeg decoder extension (built from source, not on Maven) for AC3/E-AC3/DTS on devices without hardware support.
3. Parental control with PIN on categories.
4. Fastlane lanes and Play internal testing track (see `docs/release.md`).

Decided against: a full EPG grid and catch-up playback (the now/next guide per channel in the Live list is enough).
