package com.androbito.tilfaz.core.data.mapper

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.network.dto.SeriesInfoResponseDto
import com.androbito.tilfaz.core.network.dto.VodInfoResponseDto
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.serialization.json.Json
import org.junit.Test

class DetailsMappersTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun `series info with episodes keyed by season maps to ordered seasons and episodes`() {
        val dto = json.decodeFromString<SeriesInfoResponseDto>(
            """
            {
              "seasons": [{"season_number": "1", "name": "Saison 1"}],
              "info": {"plot": "Family secrets.", "genre": "Crime, Drama", "rating": "6"},
              "episodes": {
                "1": [
                  {"id": "90102", "episode_num": "2", "title": "S01E02", "container_extension": "mp4", "season": 1, "info": {"duration_secs": "2580"}},
                  {"id": "90101", "episode_num": 1, "title": "", "container_extension": "mkv", "info": {"duration": "00:43:00", "plot": "Kama returns."}}
                ]
              }
            }
            """.trimIndent(),
        )

        val details = dto.toDomain(Fixtures.series, json)

        assertThat(details.seasons).hasSize(1)
        assertThat(details.seasons.single().name).isEqualTo("Saison 1")
        assertThat(details.seasons.single().episodes.map { it.id }).containsExactly("90101", "90102")
        assertThat(details.seasons.single().episodes.first().title).isEqualTo("S01E01")
        assertThat(details.seasons.single().episodes.first().durationSeconds).isEqualTo(2580L)
        assertThat(details.episodeCount).isEqualTo(2)
        assertThat(details.genre).isEqualTo("Crime, Drama")
    }

    @Test
    fun `movie info fills details and keeps the list row as fallback`() {
        val dto = json.decodeFromString<VodInfoResponseDto>(
            """{"info": {"plot": "A story.", "duration": "1h 50m", "backdrop_path": ["http://img/b1.jpg", "bad"], "youtube_trailer": "abc123"},
                "movie_data": {"stream_id": 501, "container_extension": "mp4"}}""",
        )

        val details = dto.toDomain(Fixtures.movie)

        assertThat(details.movie.containerExtension).isEqualTo("mp4")
        assertThat(details.movie.name).isEqualTo(Fixtures.movie.name)
        assertThat(details.durationSeconds).isEqualTo(6600L)
        assertThat(details.backdropUrls).containsExactly("http://img/b1.jpg")
        assertThat(details.trailerUrl).isEqualTo("https://www.youtube.com/watch?v=abc123")
    }
}
