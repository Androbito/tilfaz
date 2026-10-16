# Kotlin Multiplatform (KMP) reference

Same layers and dependency rule, different placement: domain and data are written once in shared Kotlin, the UI stays native (or Compose Multiplatform if the team chooses to share UI).

## Where each layer lives

| Layer | Location | Notes |
| --- | --- | --- |
| Domain | `shared/.../commonMain` | Pure Kotlin, no platform imports |
| Data | `commonMain` + `androidMain` / `iosMain` for platform pieces | Ktor client, SQLDelight or Room KMP, DataStore KMP |
| ViewModel | `commonMain` (androidx `lifecycle-viewmodel` is multiplatform) or native per platform | Share it when both UIs show the same states |
| UI | `androidApp` (Compose), `iosApp` (SwiftUI) | Or `composeApp` with Compose Multiplatform |
| DI | Koin in `commonMain` | Hilt does not run in common code |

## Module layout

```
shared/
  core/model         // entities
  core/network       // Ktor HttpClient, engines: OkHttp (android), Darwin (ios)
  core/database      // SQLDelight or Room KMP
  core/testing       // fakes for commonTest
  feature/orders     // domain + data (+ shared ViewModel) for the feature
androidApp/          // Compose UI, Android entry point
iosApp/              // SwiftUI UI, Xcode project consuming the shared framework
```

## Platform-specific code

Prefer interfaces injected from each platform over `expect`/`actual` for anything with behaviour; keep `expect`/`actual` for small primitives.

```kotlin
// commonMain
interface SecureStorage {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
}

// androidMain: EncryptedSharedPreferences / DataStore + Keystore
// iosMain: Keychain via platform.Security
```

## Shared ViewModel exposed to Swift

```kotlin
// commonMain
class OrdersViewModel(private val repository: OrderRepository) : ViewModel() {
    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()
    fun onEvent(event: OrdersEvent) { /* same UDF as android.md */ }
}
```

Use SKIE (or KMP-NativeCoroutines) so Swift sees `state` as an `AsyncSequence` and sealed `OrdersEvent` as a Swift enum:

```swift
struct OrdersView: View {
    let viewModel: OrdersViewModel
    @State private var state = OrdersUiState()

    var body: some View {
        OrdersContent(state: state) { viewModel.onEvent(event: $0) }
            .task { for await s in viewModel.state { state = s } }
    }
}
```

## Testing

- Domain, data and ViewModel tests in `commonTest` with `kotlin.test`, `kotlinx-coroutines-test`, Turbine. Run with `./gradlew allTests` (JVM + iOS simulator targets).
- Platform `actual` implementations tested in `androidUnitTest` / `iosTest`.
- UI tests stay native (Compose UI Test, XCUITest), screenshots via Paparazzi/Roborazzi and swift-snapshot-testing.

## Delivery to iOS

- Same repo: Xcode build phase calling `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.
- Separate iOS repo: publish an XCFramework via SPM (KMMBridge) with versioned releases.
- Pin Kotlin, Gradle and Xcode versions in CI; build the iOS framework in the PR pipeline so Swift API breaks are caught early.

## What to keep native

Navigation, permission prompts, widgets, app extensions, push notification handling and anything with strong platform UX conventions. Share logic first; share UI only as an explicit team decision (write an ADR).
