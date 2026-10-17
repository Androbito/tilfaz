package com.androbito.tilfaz.feature.live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.RecentChannelsRepository
import com.androbito.tilfaz.core.domain.search.TitleSearchCache
import com.androbito.tilfaz.core.domain.usecase.NowNextResolver
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.NowNext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Left column entries. The first three are virtual and exist only in the UI. */
data class LiveCategoryUi(val id: String, val name: String, val count: Int?, val kind: Kind) {
    enum class Kind { ALL, FAVORITES, HISTORY, PROVIDER }

    companion object {
        const val ALL_ID = "__all"
        const val FAVORITES_ID = "__favorites"
        const val HISTORY_ID = "__history"
    }
}

data class LiveUiState(
    val categories: List<LiveCategoryUi> = emptyList(),
    val selectedCategoryId: String = LiveCategoryUi.ALL_ID,
    val channels: List<LiveChannel> = emptyList(),
    val favoriteIds: Set<Int> = emptySet(),
    val nowNext: Map<Int, NowNext> = emptyMap(),
    val focusedChannelId: Int? = null,
    val query: String = "",
    /** While a query is typed: the best few channel names, each once, to tune straight to. */
    val suggestions: List<LiveChannel> = emptyList(),
    val isLoading: Boolean = true
) {
    val focusedChannel: LiveChannel? get() = channels.firstOrNull { it.id == focusedChannelId }
    val selectedCategory: LiveCategoryUi? get() = categories.firstOrNull { it.id == selectedCategoryId }
}

sealed interface LiveEvent {
    data class SelectCategory(val id: String) : LiveEvent
    data class FocusChannel(val id: Int) : LiveEvent
    data class QueryChanged(val query: String) : LiveEvent
    data class ToggleFavorite(val id: Int) : LiveEvent
    data class Play(val id: Int) : LiveEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val liveRepository: LiveRepository,
    favoritesRepository: FavoritesRepository,
    private val recentChannelsRepository: RecentChannelsRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val clock: Clock
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow(savedStateHandle[LiveRoute.ARG_INITIAL_CATEGORY] ?: LiveCategoryUi.ALL_ID)
    private val focusedChannelId = MutableStateFlow<Int?>(null)
    private val query = MutableStateFlow("")
    private val nowNext = MutableStateFlow<Map<Int, NowNext>>(emptyMap())
    private val epgInFlight = mutableSetOf<Int>()

    private val favoriteIds: Flow<Set<Int>> = favoritesRepository.observeKeys(ContentType.LIVE)
        .map { keys -> keys.mapNotNull(ToggleFavoriteUseCase::idFromKey).toSet() }

    private val allChannels: Flow<List<LiveChannel>> = liveRepository.observeItems(null)

    private val categories: Flow<List<LiveCategoryUi>> = combine(
        liveRepository.observeCategories(),
        allChannels,
        favoriteIds,
        recentChannelsRepository.observeRecent()
    ) { categories, channels, favorites, recent ->
        val countByCategory = channels.groupingBy { it.categoryId }.eachCount()
        buildList {
            add(LiveCategoryUi(LiveCategoryUi.ALL_ID, "", channels.size, LiveCategoryUi.Kind.ALL))
            add(LiveCategoryUi(LiveCategoryUi.FAVORITES_ID, "", favorites.size, LiveCategoryUi.Kind.FAVORITES))
            add(LiveCategoryUi(LiveCategoryUi.HISTORY_ID, "", recent.size, LiveCategoryUi.Kind.HISTORY))
            categories.forEach { add(it.toUi(countByCategory[it.id] ?: 0)) }
        }
    }

    private class Found(val channels: List<LiveChannel>, val suggestions: List<LiveChannel> = emptyList())

    private val searchCache = TitleSearchCache<LiveChannel> { it.name }

    // The whole list stays subscribed while typing, so a keystroke never reloads it; a query searches every channel.
    private val found: Flow<Found> = combine(selectedCategoryId, query.map(String::trim), allChannels) { id, q, all -> Triple(id, q, all) }
        .flatMapLatest { (id, q, all) ->
            if (q.isNotEmpty()) {
                flow {
                    val index = searchCache.indexOf(all)
                    val results = index.search(q)
                    emit(Found(results, index.distinctTitles(results, SUGGESTION_LIMIT)))
                }.flowOn(Dispatchers.Default)
            } else {
                val source: Flow<List<LiveChannel>> = when (id) {
                    LiveCategoryUi.ALL_ID -> flowOf(all)
                    LiveCategoryUi.FAVORITES_ID -> favoriteIds.map { fav -> all.filter { it.id in fav } }
                    LiveCategoryUi.HISTORY_ID -> recentChannelsRepository.observeRecent()
                    else -> liveRepository.observeItems(id)
                }
                source.map { Found(it) }
            }
        }

    val state: StateFlow<LiveUiState> = combine(
        categories,
        selectedCategoryId,
        found,
        favoriteIds,
        combine(nowNext, focusedChannelId, query) { nn, focused, q -> Triple(nn, focused, q) }
    ) { categories, selected, found, favorites, (nowNext, focused, q) ->
        val channels = found.channels
        LiveUiState(
            categories = categories,
            selectedCategoryId = selected,
            channels = channels,
            favoriteIds = favorites,
            nowNext = nowNext,
            focusedChannelId = focused?.takeIf { id -> channels.any { it.id == id } } ?: channels.firstOrNull()?.id,
            query = q,
            suggestions = found.suggestions,
            isLoading = false
        )
    }.onEach { state -> state.focusedChannelId?.let(::loadEpg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveUiState())

    fun onEvent(event: LiveEvent) {
        when (event) {
            is LiveEvent.SelectCategory -> {
                selectedCategoryId.value = event.id
                focusedChannelId.value = null
            }
            is LiveEvent.FocusChannel -> {
                focusedChannelId.value = event.id
                loadEpg(event.id)
            }
            is LiveEvent.QueryChanged -> query.value = event.query
            is LiveEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite.live(event.id) }
            is LiveEvent.Play -> viewModelScope.launch { recentChannelsRepository.record(event.id) }
        }
    }

    /** Short EPG is fetched per channel on focus and cached for the screen's lifetime; nothing is fetched for channels nobody looks at. */
    private fun loadEpg(channelId: Int) {
        if (channelId in nowNext.value || !epgInFlight.add(channelId)) return
        viewModelScope.launch {
            val result = liveRepository.shortEpg(channelId).map { NowNextResolver.resolve(it, clock.instant()) }
            epgInFlight.remove(channelId)
            nowNext.update { it + (channelId to result.getOrDefault(NowNext.Empty)) }
        }
    }

    private companion object {
        const val SUGGESTION_LIMIT = 8
    }

    private fun Category.toUi(count: Int) = LiveCategoryUi(id = id, name = name, count = count, kind = LiveCategoryUi.Kind.PROVIDER)
}
