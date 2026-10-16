package com.androbito.tilfaz.feature.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailsUiState(
    val movie: Movie? = null,
    val details: MovieDetails? = null,
    val detailsFailed: Boolean = false,
    val isFavorite: Boolean = false,
    val progress: WatchProgress? = null,
) {
    val isLoading: Boolean get() = movie == null
    val canResume: Boolean get() = progress != null && !progress.isFinished && progress.positionMs > 0
}

sealed interface MovieDetailsEvent {
    data object ToggleFavorite : MovieDetailsEvent
    data object Retry : MovieDetailsEvent
    data object StartOver : MovieDetailsEvent
}

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vodRepository: VodRepository,
    favoritesRepository: FavoritesRepository,
    private val progressRepository: WatchProgressRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    // Type-safe navigation stores route fields under their names, which keeps this testable with a plain SavedStateHandle.
    private val movieId: Int = checkNotNull(savedStateHandle[MovieDetailsRoute.ARG_MOVIE_ID])
    private val loaded = MutableStateFlow(Loaded())

    private data class Loaded(val movie: Movie? = null, val details: MovieDetails? = null, val failed: Boolean = false, val progress: WatchProgress? = null)

    val state: StateFlow<MovieDetailsUiState> = combine(
        loaded,
        favoritesRepository.observeKeys(ContentType.VOD).map { ToggleFavoriteUseCase.vodKey(movieId) in it },
    ) { loaded, favorite ->
        MovieDetailsUiState(
            movie = loaded.details?.movie ?: loaded.movie,
            details = loaded.details,
            detailsFailed = loaded.failed,
            isFavorite = favorite,
            progress = loaded.progress,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieDetailsUiState())

    init {
        load()
    }

    fun onEvent(event: MovieDetailsEvent) {
        when (event) {
            MovieDetailsEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite.movie(movieId) }
            MovieDetailsEvent.Retry -> load()
            MovieDetailsEvent.StartOver -> viewModelScope.launch {
                progressRepository.clear(ToggleFavoriteUseCase.vodKey(movieId))
                loaded.update { it.copy(progress = null) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val movie = vodRepository.movie(movieId)
            val progress = progressRepository.progress(ToggleFavoriteUseCase.vodKey(movieId))
            loaded.update { it.copy(movie = movie, progress = progress, failed = false) }
            vodRepository.details(movieId)
                .onSuccess { details -> loaded.update { it.copy(details = details) } }
                .onFailure { loaded.update { it.copy(failed = true) } }
        }
    }
}
