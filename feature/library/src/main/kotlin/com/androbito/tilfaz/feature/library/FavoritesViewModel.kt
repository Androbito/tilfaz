package com.androbito.tilfaz.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
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

/** An item the user asked to drop from favourites and has not yet confirmed. */
data class PendingRemoval(val type: ContentType, val id: Int, val name: String)

data class FavoritesUiState(
    val channels: List<LiveChannel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val pendingRemoval: PendingRemoval? = null
)

sealed interface FavoritesEvent {
    data class RequestRemoval(val type: ContentType, val id: Int, val name: String) : FavoritesEvent
    data object ConfirmRemoval : FavoritesEvent
    data object CancelRemoval : FavoritesEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    liveRepository: LiveRepository,
    vodRepository: VodRepository,
    seriesRepository: SeriesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val pending = MutableStateFlow<PendingRemoval?>(null)

    /** Only the favourited rows are loaded (never the whole catalog), sorted by name. */
    private fun <T> favouritesOf(type: ContentType, load: (Set<Int>) -> Flow<List<T>>, nameOf: (T) -> String): Flow<List<T>> =
        favoritesRepository.observeKeys(type)
            .map { keys -> keys.mapNotNull(ToggleFavoriteUseCase::idFromKey).toSet() }
            .distinctUntilChanged()
            .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(emptyList()) else load(ids) }
            .map { items -> items.sortedBy { nameOf(it).lowercase() } }

    val state: StateFlow<FavoritesUiState> = combine(
        favouritesOf(ContentType.LIVE, liveRepository::observeByIds) { it.name },
        favouritesOf(ContentType.VOD, vodRepository::observeByIds) { it.name },
        favouritesOf(ContentType.SERIES, seriesRepository::observeByIds) { it.name },
        pending
    ) { channels, movies, series, pendingRemoval ->
        FavoritesUiState(channels, movies, series, pendingRemoval)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.RequestRemoval -> pending.value = PendingRemoval(event.type, event.id, event.name)
            FavoritesEvent.CancelRemoval -> pending.value = null
            FavoritesEvent.ConfirmRemoval -> {
                val removal = pending.value ?: return
                pending.value = null
                viewModelScope.launch {
                    when (removal.type) {
                        ContentType.LIVE -> toggleFavorite.live(removal.id)
                        ContentType.VOD -> toggleFavorite.movie(removal.id)
                        ContentType.SERIES -> toggleFavorite.series(removal.id)
                    }
                }
            }
        }
    }
}
