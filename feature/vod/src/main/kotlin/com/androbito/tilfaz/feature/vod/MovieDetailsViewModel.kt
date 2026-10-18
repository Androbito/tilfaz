package com.androbito.tilfaz.feature.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.repository.WatchlistRepository
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.domain.usecase.ToggleWatchlistUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
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

data class MovieDetailsUiState(
    val movie: Movie? = null,
    val details: MovieDetails? = null,
    val detailsFailed: Boolean = false,
    val isFavorite: Boolean = false,
    val isInWatchlist: Boolean = false,
    val progress: WatchProgress? = null,
    /** "More like this": other movies from the same provider category, the only grouping Xtream guarantees. */
    val related: List<Movie> = emptyList()
) {
    val isLoading: Boolean get() = movie == null
    val canResume: Boolean get() = progress != null && !progress.isFinished && progress.positionMs > 0
}

sealed interface MovieDetailsEvent {
    data object ToggleFavorite : MovieDetailsEvent
    data object ToggleWatchlist : MovieDetailsEvent
    data object Retry : MovieDetailsEvent
    data object StartOver : MovieDetailsEvent
}

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vodRepository: VodRepository,
    favoritesRepository: FavoritesRepository,
    watchlistRepository: WatchlistRepository,
    private val progressRepository: WatchProgressRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val toggleWatchlist: ToggleWatchlistUseCase
) : ViewModel() {

    // Type-safe navigation stores route fields under their names, which keeps this testable with a plain SavedStateHandle.
    private val movieId: Int = checkNotNull(savedStateHandle[MovieDetailsRoute.ARG_MOVIE_ID])
    private val loaded = MutableStateFlow(Loaded())

    private data class Loaded(val movie: Movie? = null, val details: MovieDetails? = null, val failed: Boolean = false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val related: Flow<List<Movie>> = loaded
        .map { it.movie?.categoryId }
        .distinctUntilChanged()
        .flatMapLatest { categoryId ->
            if (categoryId == null) flowOf(emptyList()) else vodRepository.observeRelated(categoryId, excludeId = movieId, limit = RELATED_LIMIT)
        }

    // Observed, not read once: the player saves progress while this page waits underneath it on the back stack.
    private val progress: Flow<WatchProgress?> = progressRepository.observeContinueWatching(ContentType.VOD)
        .map { list -> list.firstOrNull { it.playableKey == ToggleFavoriteUseCase.vodKey(movieId) } }

    val state: StateFlow<MovieDetailsUiState> = combine(
        loaded,
        favoritesRepository.observeKeys(ContentType.VOD).map { ToggleFavoriteUseCase.vodKey(movieId) in it },
        watchlistRepository.observeKeys(ContentType.VOD).map { ToggleFavoriteUseCase.vodKey(movieId) in it },
        progress,
        related
    ) { loaded, favorite, inWatchlist, progress, related ->
        MovieDetailsUiState(
            movie = loaded.details?.movie ?: loaded.movie,
            details = loaded.details,
            detailsFailed = loaded.failed,
            isFavorite = favorite,
            isInWatchlist = inWatchlist,
            progress = progress,
            related = related
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieDetailsUiState())

    init {
        load()
    }

    fun onEvent(event: MovieDetailsEvent) {
        when (event) {
            MovieDetailsEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite.movie(movieId) }
            MovieDetailsEvent.ToggleWatchlist -> viewModelScope.launch { toggleWatchlist.movie(movieId) }
            MovieDetailsEvent.Retry -> load()
            MovieDetailsEvent.StartOver -> viewModelScope.launch { progressRepository.clear(ToggleFavoriteUseCase.vodKey(movieId)) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val movie = vodRepository.movie(movieId)
            loaded.update { it.copy(movie = movie, failed = false) }
            vodRepository.details(movieId)
                .onSuccess { details -> loaded.update { it.copy(details = details) } }
                .onFailure { loaded.update { it.copy(failed = true) } }
        }
    }

    private companion object {
        const val RELATED_LIMIT = 20
    }
}
