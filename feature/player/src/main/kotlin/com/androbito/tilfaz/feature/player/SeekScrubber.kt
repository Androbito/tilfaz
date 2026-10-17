package com.androbito.tilfaz.feature.player

import kotlin.math.abs

/**
 * A seek in progress: the cursor sits at [targetMs] while playback carries on from [originMs] until the user commits.
 * [streak] counts the presses that followed each other closely in the same direction.
 */
data class ScrubState(val originMs: Long, val targetMs: Long, val direction: Int, val streak: Int, val lastPressAtMs: Long) {
    val deltaMs: Long get() = targetMs - originMs
}

/** The speed ladder of the seek cursor: tap for 10s, hold (or tap fast) to cover a film quickly. */
object SeekScrubber {
    /** Presses closer together than this count as a hold; a longer pause starts the ladder again. */
    const val STREAK_WINDOW_MS = 450L

    fun step(state: ScrubState?, direction: Int, positionMs: Long, durationMs: Long, nowMs: Long): ScrubState {
        val continuing = state != null && state.direction == direction && nowMs - state.lastPressAtMs <= STREAK_WINDOW_MS
        val streak = if (continuing) state!!.streak + 1 else 1
        val origin = state?.originMs ?: positionMs
        val from = state?.targetMs ?: positionMs
        val target = (from + direction * stepSizeMs(streak)).coerceIn(0L, durationMs)
        return ScrubState(originMs = origin, targetMs = target, direction = direction, streak = streak, lastPressAtMs = nowMs)
    }

    fun stepSizeMs(streak: Int): Long = when {
        streak <= 3 -> 10_000L
        streak <= 8 -> 30_000L
        streak <= 14 -> 60_000L
        else -> 300_000L
    }
}

/** "+2:00", "-0:30", "+1:02:05": how far the cursor is from where playback is. */
fun formatScrubDelta(deltaMs: Long): String {
    if (deltaMs == 0L) return "0:00"
    val totalSeconds = abs(deltaMs) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    val clock = if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
    return (if (deltaMs > 0) "+" else "-") + clock
}
