package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.NowNext
import java.time.Instant

/** Picks the programme on air and the one after it from a short EPG list, tolerating unsorted or stale lists. */
object NowNextResolver {
    fun resolve(entries: List<EpgEntry>, at: Instant): NowNext {
        if (entries.isEmpty()) return NowNext.Empty
        val sorted = entries.sortedBy { it.start }
        val now = sorted.firstOrNull { it.isOnAirAt(at) }
        val next = when {
            now != null -> sorted.firstOrNull { !it.start.isBefore(now.end) }
            else -> sorted.firstOrNull { it.start.isAfter(at) }
        }
        return NowNext(now = now, next = next)
    }
}
