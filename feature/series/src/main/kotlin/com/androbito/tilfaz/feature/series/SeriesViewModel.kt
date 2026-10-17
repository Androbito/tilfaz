package com.androbito.tilfaz.feature.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.usecase.CatalogBrowser
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SeriesUiState(val browser: CatalogBrowser.Snapshot<Series>? = null)

sealed interface SeriesEvent {
    data class SelectSection(val id: String) : SeriesEvent
    data class QueryChanged(val query: String) : SeriesEvent
    data class ToggleFavorite(val seriesId: Int) : SeriesEvent
}

@HiltViewModel
class SeriesViewModel @Inject constructor(
    seriesRepository: SeriesRepository,
    favoritesRepository: FavoritesRepository,
    progressRepository: WatchProgressRepository,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val browser = CatalogBrowser(
        repository = seriesRepository,
        favoritesRepository = favoritesRepository,
        progressRepository = progressRepository,
        recent = seriesRepository::observeRecent,
        contentType = ContentType.SERIES,
        idOf = { it.id },
        nameOf = { it.name },
        favoriteKeyOf = ToggleFavoriteUseCase::seriesKey
    )

    /** The grid. Cached so it survives a rotation or a return from the details page without being reloaded. */
    val items: Flow<PagingData<Series>> = browser.items.cachedIn(viewModelScope)

    val state: StateFlow<SeriesUiState> = browser.snapshot
        .map { SeriesUiState(browser = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesUiState())

    fun onEvent(event: SeriesEvent) {
        when (event) {
            is SeriesEvent.SelectSection -> browser.select(event.id)
            is SeriesEvent.QueryChanged -> browser.search(event.query)
            is SeriesEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite.series(event.seriesId) }
        }
    }
}
