package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Playable
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) {
    suspend fun live(channelId: Int) = favoritesRepository.toggle(liveKey(channelId), ContentType.LIVE)
    suspend fun movie(movieId: Int) = favoritesRepository.toggle(vodKey(movieId), ContentType.VOD)
    suspend fun series(seriesId: Int) = favoritesRepository.toggle(seriesKey(seriesId), ContentType.SERIES)

    companion object {
        fun liveKey(channelId: Int) = "live:$channelId"
        fun vodKey(movieId: Int) = "vod:$movieId"
        fun seriesKey(seriesId: Int) = "series:$seriesId"
        fun idFromKey(key: String): Int? = key.substringAfterLast(':').toIntOrNull()
        fun keyOf(playable: Playable): String = when (playable) {
            is Playable.Live -> liveKey(playable.channel.id)
            is Playable.Film -> vodKey(playable.movie.id)
            is Playable.SeriesEpisode -> seriesKey(playable.episode.seriesId)
        }
    }
}
