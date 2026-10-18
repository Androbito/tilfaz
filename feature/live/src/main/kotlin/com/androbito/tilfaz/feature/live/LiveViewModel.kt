package com.androbito.tilfaz.feature.live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.RecentChannelsRepository
import com.androbito.tilfaz.core.domain.usecase.NowNextResolver
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.NowNext
import com.androbito.tilfaz.core.model.TitleText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Left column entries. The first two are virtual and exist only in the UI. */
data class LiveCategoryUi(val id: String, val name: String, val count: Int?, val kind: Kind) {
    enum class Kind { ALL, HISTORY, PROVIDER }

    companion object {
        const val ALL_ID = "__all"
        const val HISTORY_ID = "__history"
    }
}

data class LiveUiState(
    val categories: List<LiveCategoryUi> = emptyList(),
    val selectedCategoryId: String = LiveCategoryUi.ALL_ID,
    val favoriteIds: Set<Int> = emptySet(),
    val nowNext: Map<Int, NowNext> = emptyMap(),
    /** The channel the preview pane describes: the one with focus, or else the first of the list. */
    val focusedChannel: LiveChannel? = null,
    /** The channel whose small live video should play: the focused one, once focus has rested on it. Null while scrolling. */
    val previewChannel: LiveChannel? = null,
    /** What was typed, shown at once; the list follows once the typing pauses. */
    val query: String = "",
    /** While a query is typed: the best few channel names, each once, to tune straight to. */
    val suggestions: List<LiveChannel> = emptyList(),
    val isLoading: Boolean = true
) {
    val focusedChannelId: Int? get() = focusedChannel?.id
    val selectedCategory: LiveCategoryUi? get() = categories.firstOrNull { it.id == selectedCategoryId }
}

sealed interface LiveEvent {
    data class SelectCategory(val id: String) : LiveEvent
    data class FocusChannel(val channel: LiveChannel) : LiveEvent
    data class QueryChanged(val query: String) : LiveEvent
    data class ToggleFavorite(val id: Int) : LiveEvent
    data class Play(val id: Int) : LiveEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
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
    private val explicitFocus = MutableStateFlow<LiveChannel?>(null)
    private val query = MutableStateFlow("")
    private val nowNext = MutableStateFlow<Map<Int, NowNext>>(emptyMap())
    private val epgInFlight = mutableSetOf<Int>()

    private val favoriteIds: Flow<Set<Int>> = favoritesRepository.observeKeys(ContentType.LIVE)
        .map { keys -> keys.mapNotNull(ToggleFavoriteUseCase::idFromKey).toSet() }

    /** What was typed once the typing has paused: the part of [query] that reaches the database. */
    private val appliedQuery: Flow<String> = query.map { it.trim() }
        // Only typed text waits: opening the screen and clearing the box pass straight through.
        .debounce { text -> if (text.isEmpty()) 0L else SEARCH_DELAY_MS }
        .distinctUntilChanged()

    private val categories: Flow<List<LiveCategoryUi>> = combine(
        liveRepository.observeCategories(),
        liveRepository.observeCategoryCounts(),
        liveRepository.observeCount(),
        recentChannelsRepository.observeRecent()
    ) { categories, counts, total, recent ->
        buildList {
            add(LiveCategoryUi(LiveCategoryUi.ALL_ID, "", total, LiveCategoryUi.Kind.ALL))
            add(LiveCategoryUi(LiveCategoryUi.HISTORY_ID, "", recent.size, LiveCategoryUi.Kind.HISTORY))
            categories.forEach { add(it.toUi(counts[it.id] ?: 0)) }
        }
    }

    /**
     * One search for the list, the suggestions and the preview, tagged with what was asked so a late answer to an older query
     * is never shown. A search never holds more than a screenful of results.
     */
    private val searchResults: SharedFlow<Pair<String, List<LiveChannel>>> = appliedQuery.flatMapLatest { q ->
        if (q.isEmpty()) flowOf(q to emptyList()) else liveRepository.search(q, SEARCH_RESULT_LIMIT).map { q to it }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private fun resultsFor(q: String): Flow<List<LiveChannel>> = searchResults.filter { it.first == q }.map { it.second }

    /** The channel list, a page at a time: a catalog has far more channels than a TV has memory to hold at once. */
    val channels: Flow<PagingData<LiveChannel>> = combine(selectedCategoryId, appliedQuery) { id, q -> id to q }
        .flatMapLatest { (id, q) ->
            if (q.isNotEmpty()) {
                resultsFor(q).map { PagingData.from(it) }
            } else {
                when (id) {
                    LiveCategoryUi.ALL_ID -> liveRepository.pagedItems(null)
                    LiveCategoryUi.HISTORY_ID -> recentChannelsRepository.observeRecent().map { PagingData.from(it) }
                    else -> liveRepository.pagedItems(id)
                }
            }
        }
        .cachedIn(viewModelScope)

    /** What the preview shows when nothing has been focused: the first channel of what the list shows. */
    private val firstChannel: Flow<LiveChannel?> = combine(selectedCategoryId, appliedQuery) { id, q -> id to q }
        .flatMapLatest { (id, q) ->
            when {
                q.isNotEmpty() -> resultsFor(q).map { it.firstOrNull() }
                id == LiveCategoryUi.ALL_ID -> flow { emit(liveRepository.firstOf(null)) }
                id == LiveCategoryUi.HISTORY_ID -> recentChannelsRepository.observeRecent().map { it.firstOrNull() }
                else -> flow { emit(liveRepository.firstOf(id)) }
            }
        }

    private val suggestions: Flow<List<LiveChannel>> = searchResults.map { (_, found) -> distinctByName(found) }

    /**
     * The small video follows focus, but stops at once when focus moves and starts the new channel only after it has rested:
     * the account allows one connection and a provider keeps a cut-off one busy for a while, so scrolling must not open a stream
     * per channel passed.
     */
    private val previewChannel: Flow<LiveChannel?> = combine(explicitFocus, firstChannel) { focused, first -> focused ?: first }
        .distinctUntilChanged()
        .flatMapLatest { channel ->
            flow {
                emit(null)
                // The provider pads its list with heading entries ("===== BEIN ====="): there is nothing to play behind them.
                if (channel != null && !TitleText.isSeparator(channel.name)) {
                    delay(PREVIEW_DELAY_MS)
                    emit(channel)
                }
            }
        }

    val state: StateFlow<LiveUiState> = combine(
        categories,
        selectedCategoryId,
        favoriteIds,
        combine(nowNext, explicitFocus, firstChannel) { nn, focused, first -> Triple(nn, focused, first) },
        combine(query, suggestions, previewChannel) { q, s, preview -> Triple(q, s, preview) }
    ) { categories, selected, favorites, (nowNext, focused, first), (q, suggestions, preview) ->
        LiveUiState(
            categories = categories,
            selectedCategoryId = selected,
            favoriteIds = favorites,
            nowNext = nowNext,
            focusedChannel = focused ?: first,
            previewChannel = preview,
            query = q,
            suggestions = suggestions,
            isLoading = false
        )
    }.onEach { state -> state.focusedChannelId?.let(::loadEpg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveUiState())

    fun onEvent(event: LiveEvent) {
        when (event) {
            is LiveEvent.SelectCategory -> {
                selectedCategoryId.value = event.id
                explicitFocus.value = null
            }
            is LiveEvent.FocusChannel -> {
                explicitFocus.value = event.channel
                loadEpg(event.channel.id)
            }
            is LiveEvent.QueryChanged -> {
                query.value = event.query
                explicitFocus.value = null
            }
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

    /** The same channel often comes in several qualities and languages; one suggestion each. */
    private fun distinctByName(found: List<LiveChannel>): List<LiveChannel> {
        val seen = HashSet<String>()
        val out = ArrayList<LiveChannel>(SUGGESTION_LIMIT)
        for (channel in found) {
            if (seen.add(TitleText.normalize(channel.name))) out += channel
            if (out.size == SUGGESTION_LIMIT) break
        }
        return out
    }

    companion object {
        /** How long typing must pause before the database is asked. */
        const val SEARCH_DELAY_MS = 200L

        /** How long focus must rest on a channel before its small video starts. */
        const val PREVIEW_DELAY_MS = 800L
        private const val SUGGESTION_LIMIT = 8
        private const val SEARCH_RESULT_LIMIT = 300
    }

    private fun Category.toUi(count: Int) = LiveCategoryUi(id = id, name = name, count = count, kind = LiveCategoryUi.Kind.PROVIDER)
}
