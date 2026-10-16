# Test patterns

## Fake repository (Kotlin)

```kotlin
class FakeOrderRepository : OrderRepository {
    private val orders = MutableStateFlow<List<Order>>(emptyList())
    var refreshResult: Result<Unit> = Result.success(Unit)
    val cancelledIds = mutableListOf<String>()

    fun emit(vararg items: Order) { orders.value = items.toList() }

    override fun observeOrders(): Flow<List<Order>> = orders
    override suspend fun refresh() = refreshResult
    override suspend fun cancel(orderId: String): Result<Unit> {
        cancelledIds += orderId
        return Result.success(Unit)
    }
}
```

## ViewModel test with Turbine

```kotlin
class OrdersViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule() // sets Dispatchers.Main to a test dispatcher

    private val repository = FakeOrderRepository()

    @Test
    fun `shows error when refresh fails`() = runTest {
        repository.refreshResult = Result.failure(OrderException(OrderError.Network))
        val viewModel = OrdersViewModel(repository, CancelOrderUseCase(repository, fixedClock))

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isNotNull()
        }
    }
}

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

## Repository integration test with MockWebServer and in-memory Room

```kotlin
@RunWith(AndroidJUnit4::class)
class DefaultOrderRepositoryTest {
    private val server = MockWebServer()
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    }

    @Test fun `refresh stores orders mapped to domain`() = runTest {
        server.enqueue(MockResponse().setBody(readFixture("orders_200.json")))
        val repo = DefaultOrderRepository(buildApi(server.url("/")), db.orderDao(), StandardTestDispatcher(testScheduler))

        repo.refresh()

        val orders = repo.observeOrders().first()
        assertThat(orders).hasSize(2)
    }
}
```

Share JSON fixtures with the `api-mock-server` skill's `mocks/` folder so app tests and the local mock server use the same payloads.

## Screenshot test (Paparazzi)

```kotlin
class OrdersScreenSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @Test fun content() = paparazzi.snapshot { AppTheme { OrdersScreen(sampleState, onEvent = {}) } }
    @Test fun empty() = paparazzi.snapshot { AppTheme { OrdersScreen(OrdersUiState(), onEvent = {}) } }
    @Test fun darkRtlLargeFont() = paparazzi.snapshot {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AppTheme(darkTheme = true) { OrdersScreen(sampleState, onEvent = {}) }
        }
    }
}
```

## Swift Testing: ViewModel

```swift
@MainActor
struct OrdersViewModelTests {
    @Test func showsErrorWhenRefreshFails() async {
        let repo = FakeOrderRepository(refreshError: .network)
        let vm = OrdersViewModel(repository: repo, cancelOrder: .init(repository: repo))

        await vm.send(.refresh)

        #expect(vm.state.isLoading == false)
        #expect(vm.state.errorMessage != nil)
    }
}
```

## URLProtocol stub (iOS integration)

```swift
final class StubURLProtocol: URLProtocol {
    nonisolated(unsafe) static var handler: ((URLRequest) throws -> (HTTPURLResponse, Data))?
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        do {
            let (response, data) = try Self.handler!(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch { client?.urlProtocol(self, didFailWithError: error) }
    }
    override func stopLoading() {}
}

// let config = URLSessionConfiguration.ephemeral
// config.protocolClasses = [StubURLProtocol.self]
```

## Snapshot (swift-snapshot-testing)

```swift
@Test func ordersContent() {
    let view = OrdersContent(state: .sample, send: { _ in })
    assertSnapshot(of: UIHostingController(rootView: view), as: .image(on: .iPhone13))
    assertSnapshot(of: UIHostingController(rootView: view.environment(\.layoutDirection, .rightToLeft)),
                   as: .image(on: .iPhone13), named: "rtl")
}
```

## Maestro journey (both platforms)

```yaml
appId: com.example.app
---
- launchApp:
    clearState: true
- tapOn: "Sign in"
- inputText: "demo@example.com"
- tapOn: "Continue"
- assertVisible: "Your orders"
```

Run against the app built with the mock API flavor/scheme so journeys are deterministic.
