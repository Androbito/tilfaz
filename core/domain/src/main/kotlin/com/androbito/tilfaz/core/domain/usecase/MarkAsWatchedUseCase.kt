package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.model.WatchProgress
import javax.inject.Inject

/** Moves an item's saved position to its end, which is what "finished" means and what takes it out of Continue watching. */
class MarkAsWatchedUseCase @Inject constructor(
    private val progressRepository: WatchProgressRepository
) {
    suspend operator fun invoke(progress: WatchProgress) {
        // Without a known length there is no end to move to.
        if (progress.durationMs <= 0) return
        progressRepository.save(progress.copy(positionMs = progress.durationMs))
    }
}
