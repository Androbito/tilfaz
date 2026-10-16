package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.model.WatchProgress

/** Decides where playback should start given saved progress. Kept pure so the rule is unit-tested, not buried in the player. */
object ResumePositionPolicy {
    private const val MIN_RESUME_MS = 30_000L

    fun startPositionMs(progress: WatchProgress?, resumeEnabled: Boolean): Long {
        if (!resumeEnabled || progress == null) return 0L
        if (progress.isFinished) return 0L
        if (progress.positionMs < MIN_RESUME_MS) return 0L
        return progress.positionMs
    }
}
