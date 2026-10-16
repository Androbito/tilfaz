package com.androbito.tilfaz.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val channels: List<LiveChannel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList()
) {
    val isTooShort: Boolean get() = query.trim().length < MIN_QUERY_LENGTH
    val isEmpty: Boolean get() = !isTooShort && channels.isEmpty() && movies.isEmpty() && series.isEmpty()

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    liveRepository: LiveRepository,
    vodRepository: VodRepository,
    seriesRepository: SeriesRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val results = query
        .debounce(DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.length < SearchUiState.MIN_QUERY_LENGTH) {
                flowOf(Triple(emptyList<LiveChannel>(), emptyList<Movie>(), emptyList<Series>()))
            } else {
                combine(liveRepository.search(trimmed), vodRepository.search(trimmed), seriesRepository.search(trimmed)) { c, m, s -> Triple(c, m, s) }
            }
        }

    val state: StateFlow<SearchUiState> = combine(query, results) { q, (channels, movies, series) ->
        SearchUiState(q, channels, movies, series)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChanged(value: String) {
        query.value = value
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
    }
}
