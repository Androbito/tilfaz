# Tilfaz · تلفاز

A fast Android TV client for Xtream Codes IPTV accounts: Live TV, Movies and Series with the layout you expect
from the usual IPTV apps, but built with a cached catalog so it opens instantly, a player that can switch stream
format when a channel refuses to play, and a codebase that follows software craftsmanship rules.

![Status](https://img.shields.io/badge/status-v0.1%20scaffold-blue)

## Download

The signed APK for Android TV, Google TV, Fire TV and Android boxes is on the
[releases page](https://github.com/Androbito/tilfaz/releases/latest) (install steps are in the release notes).
On a Fire TV or Android TV, install the free Downloader app and type the code **2086257** (or open `aftv.news/2086257`).
Tilfaz is a player only: it ships with no channels or content, you sign in with the account your own provider gave you.

## Features (v1)

- Sign in with server URL, username and password. Credentials are encrypted at rest with the Android Keystore.
- Home screen with Live TV / Movies / Series tiles, per-catalog "last updated" and refresh, account expiry.
- Live: categories (All, Favourites, History + provider), channel list with now/next programme, preview pane.
- Movies and Series: categories, Recently added, Continue watching, Favourites, poster grid with rating badges,
  details pages, season/episode lists, resume where you stopped.
- Player: Media3 ExoPlayer, D-pad controls, channel zapping, error screen with Retry and "Try HLS / MPEG-TS" switch,
  configurable user agent and buffering profile.
- Search inside Live, Movies and Series: ranked results and suggestions as you type, over the cached catalog.

## Build

Requirements: JDK 17, Android Studio (Ladybug or newer) with an Android TV emulator or device on Android 8.0+.

```bash
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

No provider at hand? Run the mock panel and sign in with `http://10.0.2.2:3001` (see [mocks/README.md](mocks/README.md)).

## Project structure

Multi-module Clean Architecture with convention plugins in `build-logic/`. The map is in [CLAUDE.md](CLAUDE.md),
decisions are in [docs/adr](docs/adr/README.md).

## Engineering practices

Built with the [mobile-craft-skills](https://github.com/Androbito/mobile-craft-skills) set, vendored in `.claude/skills`:
clean architecture, TDD, test pyramid (unit, integration, screenshot), quality audit before release,
CI with GitHub Actions, API mocking, ADRs.

## Legal

Tilfaz is a player. It ships with no content and no server; you need a subscription from a provider that gives
you Xtream Codes credentials, and you are responsible for the legality of that subscription where you live.

## License

MIT
