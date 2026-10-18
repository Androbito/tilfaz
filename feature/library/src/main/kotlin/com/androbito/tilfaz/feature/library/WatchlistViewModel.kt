package com.androbito.tilfaz.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.repository.WatchlistRepository
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.domain.usecase.ToggleWatchlistUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val pendingRemoval: PendingRemoval? = null
)

sealed interface WatchlistEvent {
    data class RequestRemoval(val type: ContentType, val id: Int, val name: String) : WatchlistEvent
    data object ConfirmRemoval : WatchlistEvent
    data object CancelRemoval : WatchlistEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    vodRepository: VodRepository,
    seriesRepository: SeriesRepository,
    private val watchlistRepository: WatchlistRepository,
    private val toggleWatchlist: ToggleWatchlistUseCase
) : ViewModel() {

    private val pending = MutableStateFlow<PendingRemoval?>(null)

    /** Only the listed rows are loaded (never the whole catalog), sorted by name. */
    private fun <T> listedOf(type: ContentType, load: (Set<Int>) -> Flow<List<T>>, nameOf: (T) -> String): Flow<List<T>> =
        watchlistRepository.observeKeys(type)
            .map { keys -> keys.mapNotNull(ToggleFavoriteUseCase::idFromKey).toSet() }
            .distinctUntilChanged()
            .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(emptyList()) else load(ids) }
            .map { items -> items.sortedBy { nameOf(it).lowercase() } }

    val state: StateFlow<WatchlistUiState> = combine(
        listedOf(ContentType.VOD, vodRepository::observeByIds) { it.name },
        listedOf(ContentType.SERIES, seriesRepository::observeByIds) { it.name },
        pending
    ) { movies, series, pendingRemoval ->
        WatchlistUiState(movies, series, pendingRemoval)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState())

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.RequestRemoval -> pending.value = PendingRemoval(event.type, event.id, event.name)
            WatchlistEvent.CancelRemoval -> pending.value = null
            WatchlistEvent.ConfirmRemoval -> {
                val removal = pending.value ?: return
                pending.value = null
                viewModelScope.launch {
                    when (removal.type) {
                        ContentType.VOD -> toggleWatchlist.movie(removal.id)
                        ContentType.SERIES -> toggleWatchlist.series(removal.id)
                        ContentType.LIVE -> Unit // live channels are never on the watch list
                    }
                }
            }
        }
    }
}
