# Android reference: Kotlin, Jetpack Compose, Hilt, Coroutines

Adapt names and packages to the project. These are shapes, not copy-paste mandates.

## Domain

```kotlin
// core/model
data class Order(val id: String, val total: Money, val status: OrderStatus)

// domain: repository contract owned by the domain
interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
    suspend fun refresh(): Result<Unit>
    suspend fun cancel(orderId: String): Result<Unit>
}

sealed interface OrderError {
    data object Network : OrderError
    data object NotCancellable : OrderError
    data class Unknown(val cause: Throwable) : OrderError
}

class OrderException(val error: OrderError) : Exception()

// Use case only because it carries a rule
class CancelOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(order: Order): Result<Unit> {
        if (order.status != OrderStatus.PENDING) {
            return Result.failure(OrderException(OrderError.NotCancellable))
        }
        return repository.cancel(order.id)
    }
}
```

## Data

```kotlin
@Serializable
data class OrderDto(val id: String, val totalCents: Long, val currency: String, val status: String)

fun OrderDto.toDomain() = Order(
    id = id,
    total = Money(totalCents, currency),
    status = OrderStatus.fromApi(status),
)

class DefaultOrderRepository @Inject constructor(
    private val api: OrderApi,
    private val dao: OrderDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OrderRepository {

    // Offline-first: the database is the single source of truth
    override fun observeOrders(): Flow<List<Order>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        runCatching { dao.upsertAll(api.getOrders().map { it.toEntity() }) }
            .mapError()
    }

    override suspend fun cancel(orderId: String): Result<Unit> = withContext(io) {
        runCatching { api.cancel(orderId); dao.markCancelled(orderId) }.mapError()
    }

    private fun <T> Result<T>.mapError(): Result<T> = recoverCatching { e ->
        throw OrderException(if (e is IOException) OrderError.Network else OrderError.Unknown(e))
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OrderDataModule {
    @Binds abstract fun bindOrderRepository(impl: DefaultOrderRepository): OrderRepository
}
```

## Presentation (UDF)

```kotlin
data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderItemUi> = emptyList(),
    val errorMessage: UiText? = null,
)

sealed interface OrdersEvent {
    data object Refresh : OrdersEvent
    data class Cancel(val orderId: String) : OrdersEvent
    data object ErrorShown : OrdersEvent
}

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val cancelOrder: CancelOrderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    init {
        repository.observeOrders()
            .onEach { orders -> _state.update { it.copy(orders = orders.map(Order::toUi)) } }
            .launchIn(viewModelScope)
        onEvent(OrdersEvent.Refresh)
    }

    fun onEvent(event: OrdersEvent) {
        when (event) {
            OrdersEvent.Refresh -> viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                repository.refresh().onFailure(::showError)
                _state.update { it.copy(isLoading = false) }
            }
            is OrdersEvent.Cancel -> viewModelScope.launch { /* find order, call cancelOrder */ }
            OrdersEvent.ErrorShown -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun showError(e: Throwable) { /* map OrderError to UiText */ }
}

@Composable
fun OrdersRoute(viewModel: OrdersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OrdersScreen(state = state, onEvent = viewModel::onEvent)
}

// Stateless, previewable, screenshot-testable
@Composable
fun OrdersScreen(state: OrdersUiState, onEvent: (OrdersEvent) -> Unit) { /* ... */ }
```

Key points: `collectAsStateWithLifecycle`, a stateless `Screen` composable separate from the `Route`, injected dispatchers, `StateFlow` exposed read-only.

## Gradle conventions

Use a version catalog (`gradle/libs.versions.toml`) and convention plugins in `build-logic/` (`android-feature`, `android-library`, `jvm-library`) so every module is configured the same way. Pure domain modules should be `kotlin("jvm")` modules so they cannot touch the Android SDK.
