package com.androbito.tilfaz.core.data.mapper

import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.network.dto.EpgListingDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Xtream short EPG: titles and descriptions are base64, times come both as unix timestamps and as
 * "yyyy-MM-dd HH:mm:ss" in the server's time zone. Timestamps win; the text form is the fallback.
 */
object EpgMapper {
    private val textFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun toDomain(listings: List<EpgListingDto>, channelId: Int, serverZone: ZoneId): List<EpgEntry> = listings.mapNotNull { dto ->
        val start = dto.startTimestamp?.let { Instant.ofEpochSecond(it) } ?: parseText(dto.start, serverZone) ?: return@mapNotNull null
        val end = dto.stopTimestamp?.let { Instant.ofEpochSecond(it) } ?: parseText(dto.end, serverZone) ?: return@mapNotNull null
        if (!end.isAfter(start)) return@mapNotNull null
        EpgEntry(
            channelId = channelId,
            title = dto.title?.decodeBase64OrSelf()?.trim()?.ifEmpty { null } ?: return@mapNotNull null,
            description = dto.description?.decodeBase64OrSelf()?.trim()?.ifEmpty { null },
            start = start,
            end = end,
        )
    }

    private fun parseText(value: String?, zone: ZoneId): Instant? = value?.let {
        runCatching { LocalDateTime.parse(it.trim(), textFormat).atZone(zone).toInstant() }.getOrNull()
    }

    internal fun String.decodeBase64OrSelf(): String = runCatching {
        String(Base64.getDecoder().decode(trim()), Charsets.UTF_8)
    }.getOrElse { this }
}
