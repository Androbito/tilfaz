# Xtream mock server

Local stand-in for an Xtream Codes panel so the app can be developed and demoed without a provider,
and so error states are reproducible.

```bash
node tools/mock-server/mock-server.mjs --dir mocks --port 3001
```

Sign in from the app (emulator) with server `http://10.0.2.2:3001`, any username, any password.
Physical TV on the same Wi-Fi: `http://<laptop-ip>:3001`.

## Endpoints

| Route | Scenarios |
| --- | --- |
| `GET /player_api.php` (authentication) | `default`, `unauthorized`, `expired`, `html_block`, `server_error`, `slow`, `offline` |
| `?action=get_live_categories` / `get_live_streams` | `default`, `empty`, `server_error`, `slow`, `offline` |
| `?action=get_vod_categories` / `get_vod_streams` | same |
| `?action=get_series_categories` / `get_series` | same |
| `?action=get_vod_info&vod_id=` | `default`, `minimal`, `server_error`, `offline` |
| `?action=get_series_info&series_id=` | `default`, `no_episodes`, `server_error`, `offline` |
| `?action=get_short_epg&stream_id=` | `default` (times relative to now), `empty`, `server_error`, `offline` |
| `/live|movie|series/:user/:pass/:file` | `default` redirects to a public HLS test stream, `not_found`, `forbidden`, `offline` |

Switch a scenario: `curl -X POST localhost:3001/__mock/scenario -d '{"scenario":"slow"}'`
or per route: `-d '{"route":"GET /player_api.php?action=get_live_streams","scenario":"empty"}'`.
Reset: `curl -X POST localhost:3001/__mock/reset`.

The fixtures deliberately mix the type inconsistencies real panels produce (ints as strings, empty strings,
decimal commas, a row without an id, `backdrop_path` as string or array) because that is what the parsers are tested against.

`tools/mock-server/mock-server.mjs` is the `api-mock-server` skill's script plus two additions made for this project:
a `query` matcher on routes (Xtream multiplexes everything on `?action=`) and `{{unix±seconds}}` templating.
Both are worth upstreaming to mobile-craft-skills.
