package com.androbito.tilfaz.core.data.mapper

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.network.dto.CategoryDto
import com.androbito.tilfaz.core.network.dto.LiveStreamDto
import com.androbito.tilfaz.core.network.dto.VodStreamDto
import org.junit.Test

class CatalogMappersTest {
    @Test
    fun `rows without an id are dropped and order is preserved`() {
        val entities = listOf(
            LiveStreamDto(streamId = 10, name = "A"),
            LiveStreamDto(streamId = null, name = "ghost"),
            LiveStreamDto(streamId = 11, name = "B")
        ).toLiveEntities()

        assertThat(entities).hasSize(2)
        assertThat(entities.map { it.position }).isEqualTo(listOf(0, 2))
    }

    @Test
    fun `non http logos are discarded`() {
        val entity = listOf(LiveStreamDto(streamId = 1, name = "x", streamIcon = "data:image/png;base64,AAAA")).toLiveEntities().single()
        assertThat(entity.logoUrl).isNull()
    }

    @Test
    fun `movie year is taken from the title when the provider omits it`() {
        val entity = listOf(VodStreamDto(streamId = 1, name = "|FR| L'Être Aimé (2026)", containerExtension = ".mkv")).toMovieEntities().single()
        assertThat(entity.year).isEqualTo(2026)
        assertThat(entity.containerExtension).isEqualTo("mkv")
    }

    @Test
    fun `added timestamps in seconds and milliseconds both become millis`() {
        val seconds = listOf(VodStreamDto(streamId = 1, name = "a", added = 1_760_000_000L)).toMovieEntities().single()
        val millis = listOf(VodStreamDto(streamId = 2, name = "b", added = 1_760_000_000_000L)).toMovieEntities().single()
        assertThat(seconds.addedAtMillis).isEqualTo(1_760_000_000_000L)
        assertThat(millis.addedAtMillis).isEqualTo(1_760_000_000_000L)
    }

    @Test
    fun `category entity carries the content type`() {
        val entity = listOf(CategoryDto(categoryId = "5", categoryName = "News")).toEntities(ContentType.LIVE).single()
        assertThat(entity.toDomain().type).isEqualTo(ContentType.LIVE)
        assertThat(entity.toDomain().name).isEqualTo("News")
    }

    @Test
    fun `duration parser accepts the common provider formats`() {
        assertThat(DurationParser.parse("01:43:20")).isEqualTo(6200L)
        assertThat(DurationParser.parse("103 min")).isEqualTo(6180L)
        assertThat(DurationParser.parse("1h 43m")).isEqualTo(6180L)
        assertThat(DurationParser.parse("")).isNull()
    }
}
