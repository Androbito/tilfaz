package com.androbito.tilfaz.core.domain.search

/** Keeps the [TitleSearch] built for the latest catalog, so each keystroke reuses it instead of indexing the catalog again. */
class TitleSearchCache<T>(private val titleOf: (T) -> String) {
    private var latest: Pair<List<T>, TitleSearch<T>>? = null

    fun indexOf(items: List<T>): TitleSearch<T> {
        latest?.takeIf { it.first === items }?.let { return it.second }
        return TitleSearch(items, titleOf).also { latest = items to it }
    }
}
