package com.androbito.tilfaz.core.model

import java.time.Instant

data class EpgEntry(
    val channelId: Int,
    val title: String,
    val description: String?,
    val start: Instant,
    val end: Instant,
) {
    fun isOnAirAt(instant: Instant): Boolean = !instant.isBefore(start) && instant.isBefore(end)
    fun progressAt(instant: Instant): Float {
        val total = (end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(1)
        val elapsed = instant.toEpochMilli() - start.toEpochMilli()
        return (elapsed.toFloat() / total).coerceIn(0f, 1f)
    }
}

data class NowNext(
    val now: EpgEntry?,
    val next: EpgEntry?,
) {
    companion object {
        val Empty = NowNext(now = null, next = null)
    }
}
