# 0005. Media3 ExoPlayer with user-switchable live stream format

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub

## Context

The trigger for this project was a "reading error" in IPTV Smarters Pro on streams that play fine in another client.
With Xtream, the same channel is available as MPEG-TS (`.ts`) and HLS (`.m3u8`); providers also whitelist player user agents.
A client that lets the user flip those two knobs fixes most such errors without changing provider.

## Options considered

1. **Media3 ExoPlayer, OkHttp data source, format and user agent from settings, "switch format" action on the error screen**: Pros: first-party, HLS/TS/progressive out of the box, same OkHttp client as the API. Cons: software decoding of AC3/E-AC3/DTS needs the FFmpeg extension, which is not on Maven.
2. **libVLC (`org.videolan.android:libvlc-all`)**: Pros: plays anything. Cons: ~30 MB, its own networking stack, weaker Compose integration.
3. **Both with a player selection setting** (as Smarters does): Pros: maximum compatibility. Cons: two players to maintain; postpone until a real need appears.

## Decision

We will use Media3 ExoPlayer, prefer extension renderers so a bundled FFmpeg module is picked up automatically, store the live
format and user agent in `PlaybackSettings`, and offer "Try HLS / Try MPEG-TS" directly on the playback error screen (the choice persists).

## Consequences

- Positive: the error the user hit has a one-click remedy; the URL builder is a pure function with tests.
- Negative: some audio codecs will stay silent until the FFmpeg extension is built (backlog item).
- Follow-up: measure zapping time; consider libVLC as an optional secondary player if codec issues are frequent.
