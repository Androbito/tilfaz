---
name: api-mock-server
description: Mock backend APIs so mobile developers can build and test features before the real web service is deployed. Generates mock routes from an OpenAPI spec, a backend contract, sample JSON or a plain description; runs a local zero-dependency mock server with switchable scenarios (success, empty, 401, 404, 500, slow, offline), latency and chaos testing; can proxy and record a partially available dev API; and wires the Android or iOS app to it through a build flavor/scheme or an in-app interceptor. Use this skill whenever the user says the API or WS is not ready, not deployed, under development, wants to mock or stub an endpoint, test error or loading states, work offline, test a contract, or mentions WireMock, Prism, MockWebServer, json-server or fake backend.
---

# API Mock Server

Unblock mobile work while the backend is still being built, and make error, empty and slow states easy to test at any time. Two complementary approaches:

| Approach | When | How |
| --- | --- | --- |
| **Local mock server** (default) | Team-wide, manual QA, Maestro/XCUITest journeys, demo builds, both platforms sharing the same mocks | `scripts/mock-server.mjs`, a Node 18+ script with no dependencies |
| **In-app interceptor** | No laptop server available (testers' devices, CI without network), or single-platform quick start | OkHttp `Interceptor` (Android) or `URLProtocol` (iOS) reading the same JSON files bundled in the debug build |

Both read the same mock file format, so mocks stay one source of truth and can also feed unit/integration test fixtures.

## Workflow

1. **Find the contract.** In priority order: an OpenAPI/Swagger file (`openapi.yaml`, `swagger.json`, a URL the user gives), backend DTOs or a Postman collection, JSON examples from the backend team, or the user's description. If nothing exists, propose the contract (endpoint, method, request, response, errors) and confirm it with the user before generating mocks, because this becomes the agreement with the backend team.
2. **Create the mocks folder** at the repo root (`mocks/`) or a shared location used by both apps. Copy `scripts/mock-server.mjs` (and `openapi-to-mocks.mjs` if a spec exists) into `tools/mock-server/` in the project so the team can run it without this skill.
3. **Generate routes.**
   - With OpenAPI: `node tools/mock-server/openapi-to-mocks.mjs openapi.json --out mocks`. It uses spec examples first, then realistic sample data. It never overwrites existing files without `--force`.
   - Without: write route files by hand following the format below. Start from `templates/mocks/`.
4. **Always include failure scenarios** per endpoint: at least `default`, an empty state for lists, the relevant 4xx errors from the contract, `server_error`, `slow` and `offline`. These are what make the UI robust.
5. **Run it**: `node tools/mock-server/mock-server.mjs --dir mocks --port 3001`. Check with `curl http://localhost:3001/__mock/routes`.
6. **Wire the app** using `references/app-integration.md` (Android `mock` flavor or debug base URL, iOS `Mock` scheme + xcconfig, cleartext only in debug).
7. **Add convenience**: an npm script or Makefile target (`make mock-api`), and a short `mocks/README.md` listing endpoints and scenarios. Optionally a debug-menu screen in the app to switch scenarios via the admin endpoint.
8. **When the real API lands**: run with `--proxy https://dev-api.example.com` so deployed endpoints go to the real backend and the rest stay mocked. Add `--record` to snapshot real responses into `mocks/_recorded/` and review them as fixtures. Compare real responses with the mocks and report contract drift (renamed fields, types, missing fields) to the user.

## Mock file format

Any `*.json` file under the mocks directory (subfolders allowed; a `fixtures/` folder is ignored as routes and used for `bodyFile`). A file holds one route object or an array of them.

```json
{
  "method": "GET",
  "path": "/api/v1/orders/:id",
  "scenarios": {
    "default":   { "status": 200, "body": { "id": "{{params.id}}", "createdAt": "{{now}}" }, "delayMs": 200 },
    "not_found": { "status": 404, "body": { "error": "ORDER_NOT_FOUND" } },
    "slow":      { "status": 200, "bodyFile": "../fixtures/order.json", "delayMs": 4000 },
    "offline":   { "dropConnection": true }
  }
}
```

| Field | Meaning |
| --- | --- |
| `method` | GET, POST, PUT, PATCH, DELETE or ANY |
| `path` | Express-style `:param` or OpenAPI-style `{param}`, `*` wildcard segment. Static routes win over parameterised ones |
| `status`, `headers`, `body` | Response. `204` sends no body |
| `bodyFile` | Path to a JSON fixture, relative to the route file |
| `delayMs` | Per-scenario latency |
| `dropConnection` | Closes the socket to simulate network loss |

Templating inside strings: `{{params.x}}`, `{{query.x}}`, `{{body.x}}` (request JSON), `{{headers.x}}`, `{{uuid}}`, `{{now}}`, `{{timestamp}}`. A string that is only a token keeps the original type (arrays, numbers).

## Choosing a scenario

Highest priority first:

1. Request header `X-Mock-Scenario: not_found` (add it from a debug interceptor or a test)
2. Query parameter `?__scenario=not_found`
3. Per-route override: `POST /__mock/scenario {"route": "GET /api/v1/orders/:id", "scenario": "not_found"}`
4. Global: `POST /__mock/scenario {"scenario": "slow"}` or the `--scenario` flag

Unknown scenario names fall back to `default`. `POST /__mock/reset` clears overrides. `GET /__mock/routes` lists routes, scenarios and the active one.

## Server flags

`--dir` mocks folder (default `./mocks`) · `--port` (default 3001) · `--host` (default 0.0.0.0 so physical devices on the same Wi-Fi can connect) · `--delay` extra latency for every call · `--fail-rate 0.1` random 503s for resilience testing · `--scenario` global default · `--proxy <url>` forward unmatched requests · `--record` save proxied responses as mock files.

Mocks are re-read on every request: edit a JSON file and the next call uses it, no restart.

## Rules

- Mocks follow the contract, not the UI's wishes. If the UI needs a field the contract lacks, raise it with the user as a backend request.
- Keep realistic data: real-looking names, long strings, accents and Arabic text, prices in cents with currency, ISO dates, pagination fields.
- Never put real customer data, real tokens or secrets in mocks; they get committed.
- The mock base URL and cleartext HTTP must only exist in debug/mock builds, never in release.
- Reuse the same JSON in unit/integration tests (MockWebServer, URLProtocol stubs) so tests and manual QA agree.
