package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.domain.repository.WatchlistRepository
import com.androbito.tilfaz.core.model.ContentType
import javax.inject.Inject

class ToggleWatchlistUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) {
    suspend fun movie(movieId: Int) = watchlistRepository.toggle(ToggleFavoriteUseCase.vodKey(movieId), ContentType.VOD)
    suspend fun series(seriesId: Int) = watchlistRepository.toggle(ToggleFavoriteUseCase.seriesKey(seriesId), ContentType.SERIES)
}
