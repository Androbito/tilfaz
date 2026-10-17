package com.androbito.tilfaz.core.data.mapper

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.network.dto.EpgListingDto
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import org.junit.Test

class EpgMapperTest {
    private fun b64(s: String) = Base64.getEncoder().encodeToString(s.toByteArray())

    @Test
    fun `decodes base64 titles and prefers unix timestamps`() {
        val entries = EpgMapper.toDomain(
            listOf(
                EpgListingDto(
                    title = b64("News Hour"),
                    description = b64("Headlines"),
                    startTimestamp = 1_760_644_800L,
                    stopTimestamp = 1_760_648_400L,
                    start = "wrong",
                    end = "wrong"
                )
            ),
            channelId = 101,
            serverZone = ZoneId.of("Europe/Paris")
        )

        val entry = entries.single()
        assertThat(entry.title).isEqualTo("News Hour")
        assertThat(entry.description).isEqualTo("Headlines")
        assertThat(entry.start).isEqualTo(Instant.ofEpochSecond(1_760_644_800L))
    }

    @Test
    fun `turns escaped line breaks sent as literal text into real ones`() {
        val entries = EpgMapper.toDomain(
            listOf(
                EpgListingDto(
                    title = b64("Fencing"),
                    description = b64("Opening ceremony.\\r\\n46 nations.\\r\\n."),
                    startTimestamp = 1L,
                    stopTimestamp = 2L
                )
            ),
            channelId = 1,
            serverZone = ZoneId.of("UTC")
        )

        assertThat(entries.single().description).isEqualTo("Opening ceremony.\n46 nations.")
    }

    @Test
    fun `falls back to text dates in the server time zone and keeps plain text titles`() {
        val entries = EpgMapper.toDomain(
            listOf(EpgListingDto(title = "Plain title", start = "2026-10-16 22:00:00", end = "2026-10-16 23:00:00")),
            channelId = 101,
            serverZone = ZoneId.of("Europe/Paris")
        )

        assertThat(entries.single().start).isEqualTo(Instant.parse("2026-10-16T20:00:00Z"))
        assertThat(entries.single().title).isEqualTo("Plain title")
    }

    @Test
    fun `entries without usable times or titles are dropped`() {
        val entries = EpgMapper.toDomain(
            listOf(
                EpgListingDto(title = b64("No times")),
                EpgListingDto(title = null, startTimestamp = 1L, stopTimestamp = 2L),
                EpgListingDto(title = b64("Ends before start"), startTimestamp = 10L, stopTimestamp = 5L)
            ),
            channelId = 1,
            serverZone = ZoneId.of("UTC")
        )
        assertThat(entries).hasSize(0)
    }
}
