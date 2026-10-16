# Wiring the app to the mocks

## Host addresses

| Where the app runs | Base URL for a server on the developer's machine |
| --- | --- |
| Android emulator | `http://10.0.2.2:3001` |
| iOS simulator | `http://localhost:3001` |
| Physical device, same Wi-Fi | `http://<laptop LAN IP>:3001` (server must listen on 0.0.0.0) |
| Physical Android over USB | `adb reverse tcp:3001 tcp:3001` then `http://localhost:3001` |

## Android: `mock` product flavor

```kotlin
// app/build.gradle.kts
android {
    buildFeatures { buildConfig = true }
    flavorDimensions += "backend"
    productFlavors {
        create("mock") {
            dimension = "backend"
            applicationIdSuffix = ".mock"
            versionNameSuffix = "-mock"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3001/\"")
        }
        create("dev") {
            dimension = "backend"
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.example.com/\"")
        }
        create("prod") {
            dimension = "backend"
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com/\"")
        }
    }
}
// Exclude the mockRelease variant so a mock build can never be published
androidComponents {
    beforeVariants { if (it.flavorName == "mock" && it.buildType == "release") it.enable = false }
}
```

Allow cleartext only in the mock flavor: `app/src/mock/res/xml/network_security_config.xml`

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
</network-security-config>
```

and reference it from `app/src/mock/AndroidManifest.xml` with `android:networkSecurityConfig="@xml/network_security_config"`.

Retrofit uses `BuildConfig.API_BASE_URL`. Debug scenario header from the app (optional debug menu):

```kotlin
class MockScenarioInterceptor(private val scenario: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val s = scenario() ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header("X-Mock-Scenario", s).build())
    }
}
```

### Android in-app interceptor (no server)

Bundle the `mocks/` folder as assets of the mock flavor (`sourceSets["mock"].assets.srcDir("../mocks")`) and short-circuit OkHttp:

```kotlin
class AssetMockInterceptor(private val assets: AssetManager, private val scenario: () -> String = { "default" }) : Interceptor {
    private val routes by lazy { loadRoutes() } // parse every *.json route file under assets/ once

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val match = routes.firstOrNull { it.method == req.method && it.regex.matches(req.url.encodedPath) }
            ?: return chain.proceed(req) // or a 404 response
        val sc = match.scenarios[scenario()] ?: match.scenarios.getValue("default")
        Thread.sleep(sc.delayMs)
        return Response.Builder()
            .request(req).protocol(Protocol.HTTP_1_1)
            .code(sc.status).message("mock")
            .body(sc.bodyJson.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
```

Implement `loadRoutes()` with kotlinx.serialization, converting `:param` to a regex. Keep this class in the `mock` source set only.

## iOS: `Mock` scheme and xcconfig

1. Duplicate the Debug configuration as `Mock` (Project > Info > Configurations).
2. `Config/Mock.xcconfig`:
   ```
   API_BASE_URL = http:/$()/localhost:3001
   ```
   (`/$()/` stops xcconfig treating `//` as a comment.)
3. Info.plist: `APIBaseURL` = `$(API_BASE_URL)`; read it with `Bundle.main.object(forInfoDictionaryKey: "APIBaseURL")`.
4. ATS exception only in the Mock configuration: use a separate `Info-Mock.plist` (set `INFOPLIST_FILE` per configuration) with `NSAppTransportSecurity > NSAllowsLocalNetworking = YES`.
5. Create a `App (Mock)` scheme whose Run/Test actions use the Mock configuration. Archive keeps Release.

Scenario header from a debug menu:

```swift
var request = URLRequest(url: url)
#if MOCK
if let scenario = DebugSettings.mockScenario { request.setValue(scenario, forHTTPHeaderField: "X-Mock-Scenario") }
#endif
```

(Add `MOCK` to Active Compilation Conditions for the Mock configuration.)

### iOS in-app stub (no server)

Bundle the mocks folder as a folder reference in a Mock-only target membership and register a `URLProtocol` (see the `mobile-testing-strategy` patterns) that matches method + path against the route files and returns the scenario body. Register it via `URLSessionConfiguration.protocolClasses` only under `#if MOCK`.

## Sharing mocks between both apps

If Android and iOS live in separate repos, put `mocks/` and `tools/mock-server/` in a small shared repo (or the backend repo, next to the OpenAPI spec) added as a git submodule or fetched in CI. One contract, one set of mocks, three consumers (Android, iOS, backend contract tests).

## Alternatives worth knowing

- **Prism** (`npx @stoplight/prism-cli mock openapi.yaml`): pure OpenAPI-driven mocking with request validation. Good when the spec is complete and authoritative.
- **WireMock**: JVM-based, powerful stateful scenarios and recording; heavier setup.
- **MockWebServer / URLProtocol**: best for automated tests, not for manual QA.

The bundled script sits in between: zero install, hand-editable scenarios, proxy and record for the transition to the real API.
