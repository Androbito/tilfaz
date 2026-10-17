package com.androbito.tilfaz.feature.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Episode
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.WatchProgress
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesDetailsUiState(
    val series: Series? = null,
    val details: SeriesDetails? = null,
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val isFavorite: Boolean = false,
    val selectedSeason: Int? = null,
    val progressByEpisodeId: Map<String, WatchProgress> = emptyMap(),
    /** "More like this": other series from the same provider category, the only grouping Xtream guarantees. */
    val related: List<Series> = emptyList()
) {
    val episodes: List<Episode> get() = details?.seasons?.firstOrNull { it.number == selectedSeason }?.episodes.orEmpty()

    /**
     * What the big Play button starts: the episode most recently left unfinished; failing that, the one after the
     * episode most recently finished; failing that (nothing watched, or the last episode was the one finished), the first.
     */
    val nextEpisode: Episode? get() {
        val all = details?.seasons?.flatMap { it.episodes }.orEmpty()
        val recent = progressByEpisodeId.entries.sortedByDescending { it.value.updatedAt }
        val inProgress = recent.firstOrNull { !it.value.isFinished && it.value.positionMs > 0 }?.key
        if (inProgress != null) all.firstOrNull { it.id == inProgress }?.let { return it }
        val lastFinished = recent.firstOrNull { it.value.isFinished }?.key
        return all.getOrNull(all.indexOfFirst { it.id == lastFinished } + 1) ?: all.firstOrNull()
    }
}

sealed interface SeriesDetailsEvent {
    data class SelectSeason(val number: Int) : SeriesDetailsEvent
    data object ToggleFavorite : SeriesDetailsEvent
    data object Retry : SeriesDetailsEvent
}

@HiltViewModel
class SeriesDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seriesRepository: SeriesRepository,
    favoritesRepository: FavoritesRepository,
    progressRepository: WatchProgressRepository,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val seriesId: Int = checkNotNull(savedStateHandle[SeriesDetailsRoute.ARG_SERIES_ID])
    private val loaded = MutableStateFlow(Loaded())

    private data class Loaded(
        val series: Series? = null,
        val details: SeriesDetails? = null,
        val loading: Boolean = true,
        val failed: Boolean = false,
        val season: Int? = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val related: Flow<List<Series>> = loaded
        .map { it.series?.categoryId }
        .distinctUntilChanged()
        .flatMapLatest { categoryId ->
            if (categoryId == null) flowOf(emptyList()) else seriesRepository.observeItems(categoryId)
        }
        .map { list -> list.filter { it.id != seriesId }.take(RELATED_LIMIT) }

    val state: StateFlow<SeriesDetailsUiState> = combine(
        loaded,
        favoritesRepository.observeKeys(ContentType.SERIES).map { ToggleFavoriteUseCase.seriesKey(seriesId) in it },
        progressRepository.observeByKeyPrefix("series:$seriesId:").map { list ->
            list.associateBy { it.playableKey.substringAfterLast(':') }
        },
        related
    ) { loaded, favorite, progress, related ->
        SeriesDetailsUiState(
            series = loaded.details?.series ?: loaded.series,
            details = loaded.details,
            isLoading = loaded.loading,
            failed = loaded.failed,
            isFavorite = favorite,
            selectedSeason = loaded.season ?: loaded.details?.seasons?.firstOrNull()?.number,
            progressByEpisodeId = progress,
            related = related
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesDetailsUiState())

    init {
        load()
    }

    fun onEvent(event: SeriesDetailsEvent) {
        when (event) {
            is SeriesDetailsEvent.SelectSeason -> loaded.update { it.copy(season = event.number) }
            SeriesDetailsEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite.series(seriesId) }
            SeriesDetailsEvent.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            loaded.update { it.copy(loading = true, failed = false, series = seriesRepository.series(seriesId)) }
            seriesRepository.details(seriesId)
                .onSuccess { details -> loaded.update { it.copy(details = details, loading = false) } }
                .onFailure { loaded.update { it.copy(loading = false, failed = true) } }
        }
    }

    private companion object {
        const val RELATED_LIMIT = 20
    }
}
