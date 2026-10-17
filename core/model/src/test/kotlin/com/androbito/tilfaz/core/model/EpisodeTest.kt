package com.androbito.tilfaz.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class EpisodeTest {
    private fun episode(title: String, season: Int = 1, number: Int = 1) = Episode(
        id = "1",
        seriesId = 9,
        seasonNumber = season,
        episodeNumber = number,
        title = title,
        containerExtension = "mkv",
        durationSeconds = null,
        plot = null,
        thumbnailUrl = null,
        rating = null
    )

    @Test
    fun `a real title follows the code`() {
        assertThat(episode("The Return", season = 2, number = 4).displayTitle).isEqualTo("S02E04  The Return")
    }

    @Test
    fun `a title that is just the code is not repeated`() {
        assertThat(episode("S01E01").displayTitle).isEqualTo("S01E01")
        assertThat(episode(" s01e01 ").displayTitle).isEqualTo("S01E01")
    }

    @Test
    fun `a title that already starts with the code is used as it is`() {
        assertThat(episode("S01E03 - The Plan", number = 3).displayTitle).isEqualTo("S01E03 - The Plan")
    }

    @Test
    fun `a blank title falls back to the code`() {
        assertThat(episode("   ").displayTitle).isEqualTo("S01E01")
    }
}
