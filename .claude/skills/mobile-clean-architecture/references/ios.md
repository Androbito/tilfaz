# iOS reference: Swift, SwiftUI, async/await, Observation

Adapt to the project. If the team already uses TCA (The Composable Architecture), keep TCA: it is a valid UDF implementation, and these layering rules still apply to its dependencies.

## Domain

```swift
// Domain package: no UIKit, SwiftUI, Foundation networking types in signatures
struct Order: Equatable, Identifiable, Sendable {
    let id: String
    let total: Money
    let status: OrderStatus
}

enum OrderError: Error, Equatable {
    case network
    case notCancellable
    case unknown
}

protocol OrderRepository: Sendable {
    func orders() -> AsyncStream<[Order]>
    func refresh() async throws(OrderError)
    func cancel(orderID: String) async throws(OrderError)
}

struct CancelOrderUseCase: Sendable {
    let repository: OrderRepository

    func callAsFunction(_ order: Order) async throws(OrderError) {
        guard order.status == .pending else { throw .notCancellable }
        try await repository.cancel(orderID: order.id)
    }
}
```

## Data

```swift
struct OrderDTO: Decodable {
    let id: String
    let totalCents: Int
    let currency: String
    let status: String
}

extension OrderDTO {
    func toDomain() -> Order {
        Order(id: id, total: Money(cents: totalCents, currency: currency),
              status: OrderStatus(apiValue: status))
    }
}

actor DefaultOrderRepository: OrderRepository {
    private let client: HTTPClient      // protocol, injectable
    private let store: OrderStore        // local persistence, single source of truth

    init(client: HTTPClient, store: OrderStore) {
        self.client = client
        self.store = store
    }

    nonisolated func orders() -> AsyncStream<[Order]> { store.observe() }

    func refresh() async throws(OrderError) {
        do {
            let dtos: [OrderDTO] = try await client.get("/orders")
            try await store.replaceAll(dtos.map { $0.toDomain() })
        } catch is URLError {
            throw .network
        } catch {
            throw .unknown
        }
    }

    func cancel(orderID: String) async throws(OrderError) { /* ... */ }
}
```

## Presentation (UDF with Observation)

```swift
@MainActor
@Observable
final class OrdersViewModel {
    struct State: Equatable {
        var isLoading = false
        var orders: [OrderRowModel] = []
        var errorMessage: String?
    }

    enum Event {
        case onAppear
        case refresh
        case cancel(orderID: String)
        case dismissError
    }

    private(set) var state = State()

    private let repository: OrderRepository
    private let cancelOrder: CancelOrderUseCase

    init(repository: OrderRepository, cancelOrder: CancelOrderUseCase) {
        self.repository = repository
        self.cancelOrder = cancelOrder
    }

    func send(_ event: Event) async {
        switch event {
        case .onAppear:
            Task { for await orders in repository.orders() { state.orders = orders.map(OrderRowModel.init) } }
            await send(.refresh)
        case .refresh:
            state.isLoading = true
            defer { state.isLoading = false }
            do { try await repository.refresh() } catch { state.errorMessage = error.userMessage }
        case .cancel(let id):
            _ = id // find order, call cancelOrder
        case .dismissError:
            state.errorMessage = nil
        }
    }
}

struct OrdersView: View {
    @State var viewModel: OrdersViewModel

    var body: some View {
        OrdersContent(state: viewModel.state) { event in
            Task { await viewModel.send(event) }
        }
        .task { await viewModel.send(.onAppear) }
    }
}

// Stateless content view: previewable and snapshot-testable
struct OrdersContent: View {
    let state: OrdersViewModel.State
    let send: (OrdersViewModel.Event) -> Void
    var body: some View { /* ... */ }
}
```

Key points: `private(set)` state, a single `send(_:)` entry point, `@MainActor` view model, `actor` or `Sendable` repositories, a stateless content view.

## Package layout

```
App/                         // Xcode app target: composition root, DI, navigation
Packages/
  Core/                      // Swift package with several targets
    Sources/Model
    Sources/Networking
    Sources/Persistence
    Sources/DesignSystem
    Sources/TestSupport
  Features/
    Sources/Orders
    Sources/Profile
```

Each target declares only the dependencies it needs in `Package.swift`, which enforces the dependency rule at compile time.
