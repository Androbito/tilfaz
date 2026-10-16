package com.androbito.tilfaz.feature.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.usecase.CatalogBrowser
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoviesUiState(
    val browser: CatalogBrowser.Snapshot<Movie>? = null
) {
    val isLoading: Boolean get() = browser == null
}

sealed interface MoviesEvent {
    data class SelectSection(val id: String) : MoviesEvent
    data class QueryChanged(val query: String) : MoviesEvent
    data class ToggleFavorite(val movieId: Int) : MoviesEvent
}

@HiltViewModel
class MoviesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vodRepository: VodRepository,
    favoritesRepository: FavoritesRepository,
    progressRepository: WatchProgressRepository,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val browser = CatalogBrowser(
        repository = vodRepository,
        favoritesRepository = favoritesRepository,
        progressRepository = progressRepository,
        contentType = ContentType.VOD,
        idOf = { it.id },
        nameOf = { it.name },
        categoryOf = { it.categoryId },
        addedAtMillisOf = { it.addedAt?.toEpochMilli() },
        favoriteKeyOf = ToggleFavoriteUseCase::vodKey
    )

    init {
        savedStateHandle.get<String>(MoviesRoute.ARG_INITIAL_SECTION)?.let(browser::select)
    }

    val state: StateFlow<MoviesUiState> = browser.snapshot
        .map { MoviesUiState(browser = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoviesUiState())

    fun onEvent(event: MoviesEvent) {
        when (event) {
            is MoviesEvent.SelectSection -> browser.select(event.id)
            is MoviesEvent.QueryChanged -> browser.search(event.query)
            is MoviesEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite.movie(event.movieId) }
        }
    }
}
