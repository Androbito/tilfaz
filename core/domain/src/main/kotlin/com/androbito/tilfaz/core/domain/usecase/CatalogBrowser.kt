package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.domain.repository.CatalogRepository
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.search.TitleSearchCache
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Browsing logic shared by the Movies and Series screens: virtual sections (All, Favourites,
 * Continue watching, Recently added) ahead of the provider categories, selection, and text filtering.
 * Pure flows, so ViewModels stay thin and this is unit-tested once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogBrowser<T>(
    private val repository: CatalogRepository<T>,
    favoritesRepository: FavoritesRepository,
    progressRepository: WatchProgressRepository,
    private val contentType: ContentType,
    private val idOf: (T) -> Int,
    private val nameOf: (T) -> String,
    private val categoryOf: (T) -> String?,
    private val addedAtMillisOf: (T) -> Long?,
    private val favoriteKeyOf: (Int) -> String,
    /** Where the search runs: indexing a catalog of a hundred thousand titles must not happen on the main thread. */
    private val computation: CoroutineDispatcher = Dispatchers.Default
) {
    data class Section(val id: String, val name: String, val count: Int?, val kind: Kind) {
        enum class Kind { ALL, FAVORITES, CONTINUE_WATCHING, RECENTLY_ADDED, PROVIDER }
    }

    data class Snapshot<T>(
        val sections: List<Section>,
        val selectedSectionId: String,
        val items: List<T>,
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

    private val all: Flow<List<T>> = repository.observeItems(null)
    private val favoriteIds: Flow<Set<Int>> = favoritesRepository.observeKeys(contentType)
        .map { keys -> keys.mapNotNull(ToggleFavoriteUseCase::idFromKey).toSet() }
    private val progress: Flow<Map<Int, WatchProgress>> = progressRepository.observeContinueWatching(contentType)
        .map { list -> list.mapNotNull { p -> ToggleFavoriteUseCase.idFromKey(p.playableKey)?.let { it to p } }.toMap() }

    private val sections: Flow<List<Section>> = combine(repository.observeCategories(), all, favoriteIds, progress) { categories, items, favorites, progress ->
        val counts = items.groupingBy(categoryOf).eachCount()
        buildList {
            add(Section(ALL, "", items.size, Section.Kind.ALL))
            add(Section(FAVORITES, "", favorites.size, Section.Kind.FAVORITES))
            add(Section(CONTINUE_WATCHING, "", progress.size, Section.Kind.CONTINUE_WATCHING))
            add(Section(RECENTLY_ADDED, "", minOf(RECENT_LIMIT, items.size), Section.Kind.RECENTLY_ADDED))
            categories.forEach { add(it.toSection(counts[it.id] ?: 0)) }
        }
    }

    private class Found<T>(val items: List<T>, val suggestions: List<T> = emptyList())

    private val searchCache = TitleSearchCache(nameOf)

    // The whole catalog stays subscribed while typing, so a keystroke never reloads it from the database.
    private val found: Flow<Found<T>> = combine(selectedSectionId, query.map(String::trim), all) { id, q, everything ->
        Triple(id, q, everything)
    }.flatMapLatest { (id, q, everything) ->
        if (q.isNotEmpty()) {
            flow {
                val index = searchCache.indexOf(everything)
                val results = index.search(q)
                emit(Found(results, index.distinctTitles(results, SUGGESTION_LIMIT)))
            }.flowOn(computation)
        } else {
            val source: Flow<List<T>> = when (id) {
                ALL -> flowOf(everything)
                FAVORITES -> favoriteIds.map { fav -> everything.filter { idOf(it) in fav } }
                CONTINUE_WATCHING -> progress.map { progress ->
                    val byId = everything.associateBy(idOf)
                    progress.keys.mapNotNull { byId[it] }
                }
                RECENTLY_ADDED -> flowOf(everything.sortedByDescending { addedAtMillisOf(it) ?: Long.MIN_VALUE }.take(RECENT_LIMIT))
                else -> repository.observeItems(id)
            }
            source.map { Found(it) }
        }
    }

    val snapshot: Flow<Snapshot<T>> =
        combine(
            sections,
            selectedSectionId,
            found,
            favoriteIds,
            combine(progress, query) { p, q ->
                p to q
            }
        ) { sections, selected, found, favorites, (progress, q) ->
            Snapshot(sections, selected, found.items, favorites, progress, q, found.suggestions)
        }

    fun select(sectionId: String) {
        selectedSectionId.value = sectionId
    }

    fun search(text: String) {
        query.value = text
    }

    fun favoriteKey(id: Int): String = favoriteKeyOf(id)

    private fun Category.toSection(count: Int) = Section(id = id, name = name, count = count, kind = Section.Kind.PROVIDER)

    companion object {
        const val ALL = "__all"
        const val FAVORITES = "__favorites"
        const val CONTINUE_WATCHING = "__continue"
        const val RECENTLY_ADDED = "__recent"
        const val RECENT_LIMIT = 60
        const val SUGGESTION_LIMIT = 8
    }
}
