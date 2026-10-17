package com.androbito.tilfaz.core.domain.usecase

import androidx.paging.PagingData
import com.androbito.tilfaz.core.domain.repository.CatalogRepository
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.model.WatchProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Browsing logic shared by the Movies and Series screens: virtual sections (All, Favourites, Continue watching,
 * Recently added) ahead of the provider categories, selection, and search.
 *
 * It never holds the catalog: counts come from the database, the grid arrives as pages ([items]), and a search asks the
 * database for the best few matches. A catalog can have well over a hundred thousand titles and a TV little free memory.
 * Pure flows, so ViewModels stay thin and this is unit-tested once.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogBrowser<T : Any>(
    private val repository: CatalogRepository<T>,
    private val recent: (limit: Int) -> Flow<List<T>>,
    favoritesRepository: FavoritesRepository,
    progressRepository: WatchProgressRepository,
    private val contentType: ContentType,
    private val idOf: (T) -> Int,
    private val nameOf: (T) -> String,
    private val favoriteKeyOf: (Int) -> String,
    /** How long typing must pause before the database is asked; keeps a fast typist from running a query per letter. */
    private val searchDelayMs: Long = SEARCH_DELAY_MS
) {
    data class Section(val id: String, val name: String, val count: Int?, val kind: Kind) {
        enum class Kind { ALL, FAVORITES, CONTINUE_WATCHING, RECENTLY_ADDED, PROVIDER }
    }

    /** Everything about the screen except the grid itself, which is [items]: it must not change when a favourite does. */
    data class Snapshot<T>(
        val sections: List<Section>,
        val selectedSectionId: String,
        val favoriteIds: Set<Int>,
        val progressById: Map<Int, WatchProgress>,
        val query: String,
        /** While a query is typed: the best few titles, each once, to jump to without typing the rest. */
        val suggestions: List<T> = emptyList()
    ) {
        val selectedSection: Section? get() = sections.firstOrNull { it.id == selectedSectionId }
    }

    val selectedSectionId = MutableStateFlow(ALL)
    val query = MutableStateFlow("")

    private val favoriteIds: Flow<Set<Int>> = favoritesRepository.observeKeys(contentType)
        .map { keys -> keys.mapNotNull(ToggleFavoriteUseCase::idFromKey).toSet() }
    private val progress: Flow<Map<Int, WatchProgress>> = progressRepository.observeContinueWatching(contentType)
        .map { list -> list.mapNotNull { p -> ToggleFavoriteUseCase.idFromKey(p.playableKey)?.let { it to p } }.toMap() }

    /** What was typed, once the typing has paused: the part of [query] that reaches the database. */
    private val appliedQuery: Flow<String> = query.map { it.trim() }
        // Only typed text waits: opening the screen and clearing the box pass straight through.
        .debounce { text -> if (text.isEmpty()) 0L else searchDelayMs }
        .distinctUntilChanged()

    private val sections: Flow<List<Section>> = combine(
        repository.observeCategories(),
        repository.observeCategoryCounts(),
        repository.observeCount(),
        favoriteIds,
        progress
    ) { categories, counts, total, favorites, progress ->
        buildList {
            add(Section(ALL, "", total, Section.Kind.ALL))
            add(Section(FAVORITES, "", favorites.size, Section.Kind.FAVORITES))
            add(Section(CONTINUE_WATCHING, "", progress.size, Section.Kind.CONTINUE_WATCHING))
            add(Section(RECENTLY_ADDED, "", minOf(RECENT_LIMIT, total), Section.Kind.RECENTLY_ADDED))
            categories.forEach { add(it.toSection(counts[it.id] ?: 0)) }
        }
    }

    /** The grid. A new value only when the section or the (settled) query changes. */
    val items: Flow<PagingData<T>> = combine(selectedSectionId, appliedQuery) { id, q -> id to q }.flatMapLatest { (id, q) ->
        if (q.isNotEmpty()) {
            repository.search(q, SEARCH_RESULT_LIMIT).map { PagingData.from(it) }
        } else {
            when (id) {
                ALL -> repository.pagedItems(null)
                FAVORITES -> favoriteIds.flatMapLatest { ids -> repository.observeByIds(ids) }.map { PagingData.from(it) }
                CONTINUE_WATCHING -> progress.flatMapLatest { progress ->
                    repository.observeByIds(progress.keys).map { found ->
                        val byId = found.associateBy(idOf)
                        PagingData.from(progress.keys.mapNotNull { byId[it] })
                    }
                }
                RECENTLY_ADDED -> recent(RECENT_LIMIT).map { PagingData.from(it) }
                else -> repository.pagedItems(id)
            }
        }
    }

    private val suggestions: Flow<List<T>> = appliedQuery.flatMapLatest { q ->
        if (q.isEmpty()) {
            flowOf(emptyList())
        } else {
            repository.search(q, SUGGESTION_FETCH).map { found -> distinctTitles(found) }
        }
    }

    val snapshot: Flow<Snapshot<T>> = combine(
        sections,
        selectedSectionId,
        favoriteIds,
        combine(progress, query, suggestions) { p, q, s -> Triple(p, q, s) }
    ) { sections, selected, favorites, (progress, q, suggestions) ->
        Snapshot(sections, selected, favorites, progress, q, suggestions)
    }

    fun select(sectionId: String) {
        selectedSectionId.value = sectionId
    }

    fun search(text: String) {
        query.value = text
    }

    fun favoriteKey(id: Int): String = favoriteKeyOf(id)

    /** The same film comes in several languages; one suggestion each. */
    private fun distinctTitles(found: List<T>): List<T> {
        val seen = HashSet<String>()
        val out = ArrayList<T>(SUGGESTION_LIMIT)
        for (item in found) {
            if (seen.add(TitleText.normalize(nameOf(item)))) out += item
            if (out.size == SUGGESTION_LIMIT) break
        }
        return out
    }

    private fun Category.toSection(count: Int) = Section(id = id, name = name, count = count, kind = Section.Kind.PROVIDER)

    companion object {
        const val ALL = "__all"
        const val FAVORITES = "__favorites"
        const val CONTINUE_WATCHING = "__continue"
        const val RECENTLY_ADDED = "__recent"
        const val RECENT_LIMIT = 60
        const val SUGGESTION_LIMIT = 8
        const val SEARCH_RESULT_LIMIT = 300
        private const val SUGGESTION_FETCH = 40
        private const val SEARCH_DELAY_MS = 200L
    }
}
