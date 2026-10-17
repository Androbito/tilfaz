package com.androbito.tilfaz.core.domain.search

/**
 * Finds titles as the user types them: the words can come in any order and each only needs its beginning, with a title that
 * starts with the query ahead of one that merely has a word starting with it, and a match inside a word last of all.
 *
 * Built once per catalog snapshot, because every title is normalised up front so a query is a quick scan over plain strings.
 */
class TitleSearch<T>(private val items: List<T>, private val titleOf: (T) -> String) {
    // Each title is stored with a leading space, so "a word starts with x" is a plain contains(" x").
    private val padded: Array<String> = Array(items.size) { " " + TitleText.normalize(titleOf(items[it])) }

    fun search(query: String, limit: Int = Int.MAX_VALUE): List<T> {
        val q = TitleText.normalize(query)
        if (q.isEmpty() || limit <= 0) return emptyList()
        val tokens = q.split(' ')
        val wholeStart = " $q"

        val keys = LongArray(items.size)
        var count = 0
        for (index in padded.indices) {
            val title = padded[index]
            val tier = when {
                title.startsWith(wholeStart) -> when {
                    title.length == wholeStart.length -> EXACT
                    title[wholeStart.length] == ' ' -> STARTS_WITH_WORD
                    else -> STARTS_WITH
                }
                tokens.all { title.contains(" $it") } -> WORD_STARTS
                tokens.all { title.contains(it) } -> INSIDE_WORD
                else -> continue
            }
            keys[count++] = (tier shl TIER_SHIFT) or (minOf(title.length, MAX_LENGTH).toLong() shl LENGTH_SHIFT) or index.toLong()
        }
        keys.sort(0, count)
        return List(minOf(count, limit)) { items[(keys[it] and INDEX_MASK).toInt()] }
    }

    /** At most [limit] results with each title once, so the same film in three languages is not offered three times. */
    fun suggest(query: String, limit: Int): List<T> = distinctTitles(search(query), limit)

    /** The first [limit] of [results] whose titles differ, keeping their order. */
    fun distinctTitles(results: List<T>, limit: Int): List<T> {
        val seen = HashSet<String>()
        val out = ArrayList<T>(limit)
        for (item in results) {
            if (seen.add(TitleText.normalize(titleOf(item)))) out += item
            if (out.size == limit) break
        }
        return out
    }

    private companion object {
        const val EXACT = 0L
        const val STARTS_WITH_WORD = 1L
        const val STARTS_WITH = 2L
        const val WORD_STARTS = 3L
        const val INSIDE_WORD = 4L
        const val TIER_SHIFT = 48
        const val LENGTH_SHIFT = 32
        const val MAX_LENGTH = 0xFFFF
        const val INDEX_MASK = 0xFFFF_FFFFL
    }
}
