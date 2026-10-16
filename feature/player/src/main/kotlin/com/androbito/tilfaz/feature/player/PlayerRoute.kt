package com.androbito.tilfaz.feature.player

import kotlinx.serialization.Serializable

/**
 * One route for every kind of content. The player resolves the item from the catalog so the
 * navigation argument stays tiny and the stream URL (with credentials) never travels through the back stack.
 */
@Serializable
data class PlayerRoute(
    val kind: String,
    val itemId: String,
    val parentId: Int = 0,
    val categoryId: String? = null,
    val startPositionMs: Long = 0L
) {
    companion object {
        const val KIND_LIVE = "live"
        const val KIND_MOVIE = "movie"
        const val KIND_EPISODE = "episode"

        const val ARG_KIND = "kind"
        const val ARG_ITEM_ID = "itemId"
        const val ARG_PARENT_ID = "parentId"
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_START_POSITION = "startPositionMs"

        fun live(channelId: Int, categoryId: String?) = PlayerRoute(KIND_LIVE, channelId.toString(), categoryId = categoryId)
        fun movie(movieId: Int, startPositionMs: Long) = PlayerRoute(KIND_MOVIE, movieId.toString(), startPositionMs = startPositionMs)
        fun episode(
            seriesId: Int,
            episodeId: String,
            startPositionMs: Long
        ) = PlayerRoute(KIND_EPISODE, episodeId, parentId = seriesId, startPositionMs = startPositionMs)
    }
}
