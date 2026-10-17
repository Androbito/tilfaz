package com.androbito.tilfaz.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.usecase.MarkAsWatchedUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContinueMovie(val movie: Movie, val progress: WatchProgress)

/** A show is listed once, at the episode watched last. */
data class ContinueShow(val series: Series, val progress: WatchProgress, val episodeId: String)

/** An item the user asked to mark as watched and has not yet confirmed. */
data class PendingWatched(val progress: WatchProgress, val name: String)

data class ContinueWatchingUiState(
    val movies: List<ContinueMovie> = emptyList(),
    val shows: List<ContinueShow> = emptyList(),
    val pendingWatched: PendingWatched? = null
)

sealed interface ContinueWatchingEvent {
    data class RequestMarkWatched(val progress: WatchProgress, val name: String) : ContinueWatchingEvent
    data object ConfirmMarkWatched : ContinueWatchingEvent
    data object CancelMarkWatched : ContinueWatchingEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContinueWatchingViewModel @Inject constructor(
    progressRepository: WatchProgressRepository,
    vodRepository: VodRepository,
    seriesRepository: SeriesRepository,
    private val markAsWatched: MarkAsWatchedUseCase
) : ViewModel() {

    private val pending = MutableStateFlow<PendingWatched?>(null)

    private val movies = progressRepository.observeContinueWatching(ContentType.VOD).flatMapLatest { progress ->
        val ids = progress.mapNotNull { it.reference.toIntOrNull() }.toSet()
        val catalog = if (ids.isEmpty()) flowOf(emptyList()) else vodRepository.observeByIds(ids)
        catalog.map { items ->
            val byId = items.associateBy { it.id }
            // Items the catalog no longer has are skipped; the progress order (most recent first) is kept.
            progress.mapNotNull { p -> p.reference.toIntOrNull()?.let(byId::get)?.let { ContinueMovie(it, p) } }
        }
    }

    private val shows = progressRepository.observeContinueWatching(ContentType.SERIES).flatMapLatest { progress ->
        val latestPerShow = progress.distinctBy { it.seriesId() }.filter { it.seriesId() != null }
        val ids = latestPerShow.mapNotNull { it.seriesId() }.toSet()
        val catalog = if (ids.isEmpty()) flowOf(emptyList()) else seriesRepository.observeByIds(ids)
        catalog.map { items ->
            val byId = items.associateBy { it.id }
            latestPerShow.mapNotNull { p -> byId[p.seriesId()]?.let { ContinueShow(it, p, p.reference.substringAfter(':')) } }
        }
    }

    val state: StateFlow<ContinueWatchingUiState> = combine(movies, shows, pending) { movies, shows, pendingWatched ->
        ContinueWatchingUiState(movies, shows, pendingWatched)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContinueWatchingUiState())

    fun onEvent(event: ContinueWatchingEvent) {
        when (event) {
            is ContinueWatchingEvent.RequestMarkWatched -> pending.value = PendingWatched(event.progress, event.name)
            ContinueWatchingEvent.CancelMarkWatched -> pending.value = null
            ContinueWatchingEvent.ConfirmMarkWatched -> {
                val item = pending.value ?: return
                pending.value = null
                viewModelScope.launch { markAsWatched(item.progress) }
            }
        }
    }

    /** Episode progress is referenced as "seriesId:episodeId". */
    private fun WatchProgress.seriesId(): Int? = reference.substringBefore(':').toIntOrNull()
}
