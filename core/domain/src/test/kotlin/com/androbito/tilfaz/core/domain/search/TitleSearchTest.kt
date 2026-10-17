package com.androbito.tilfaz.core.domain.search

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test

class TitleSearchTest {
    private val titles = listOf(
        "|FR| Scream 4 (2011)",
        "|EN| Scream 4 (2011)",
        "|ES| Scream 4 (2011)",
        "|FR| La Vénus électrique (2026)",
        "|EN| Screamers",
        "|FR| Les Spécialistes (2026)",
        "|EN| Dream Scream Machine",
        "| The Mysterious Sound! | 2026 | ده صوت",
        "|FR| Backrooms (2026)"
    )

    private val search = TitleSearch(titles) { it }

    private fun TitleSearch<String>.found(query: String, limit: Int = 50) = search(query, limit)

    @Test
    fun `typing the first letters finds the titles with a word that starts with them`() {
        val found = search.found("scr")

        assertThat(found.size).isEqualTo(5)
        assertThat(found.all { it.contains("Scream", ignoreCase = true) }).isEqualTo(true)
    }

    @Test
    fun `a title that starts with the query comes before one that only has a word starting with it`() {
        val found = search.found("scream")

        assertThat(found.indexOf("|EN| Dream Scream Machine") > found.indexOf("|FR| Scream 4 (2011)")).isEqualTo(true)
    }

    @Test
    fun `a word start beats a match inside a word`() {
        val found = TitleSearch(listOf("Upstream", "Stream Team", "The Stream")) { it }.found("stream")

        assertThat(found).isEqualTo(listOf("Stream Team", "The Stream", "Upstream"))
    }

    @Test
    fun `the words can be typed in any order and each can be a beginning`() {
        assertThat(search.found("elec venus")).isEqualTo(listOf("|FR| La Vénus électrique (2026)"))
    }

    @Test
    fun `accents and case in the query do not matter`() {
        assertThat(search.found("SPECIALISTES")).isEqualTo(listOf("|FR| Les Spécialistes (2026)"))
        assertThat(search.found("venus")).isEqualTo(listOf("|FR| La Vénus électrique (2026)"))
    }

    @Test
    fun `the language tag is not searched`() {
        assertThat(search.found("fr")).isEmpty()
    }

    @Test
    fun `the year and arabic text are searchable`() {
        assertThat(search.found("2011").size).isEqualTo(3)
        assertThat(search.found("صوت")).isEqualTo(listOf("| The Mysterious Sound! | 2026 | ده صوت"))
    }

    @Test
    fun `a match in the middle of a word still finds the title as a last resort`() {
        assertThat(search.found("ackroo")).isEqualTo(listOf("|FR| Backrooms (2026)"))
    }

    @Test
    fun `a blank query finds nothing`() {
        assertThat(search.found("")).isEmpty()
        assertThat(search.found("   ")).isEmpty()
    }

    @Test
    fun `an unknown title finds nothing`() {
        assertThat(search.found("zzzz")).isEmpty()
    }

    @Test
    fun `the number of results is capped`() {
        assertThat(search.found("s", limit = 2).size).isEqualTo(2)
    }

    @Test
    fun `equal matches keep the catalog order`() {
        val found = search.found("scream 4")

        assertThat(found).isEqualTo(listOf("|FR| Scream 4 (2011)", "|EN| Scream 4 (2011)", "|ES| Scream 4 (2011)"))
    }

    @Test
    fun `suggestions list each title once even when it exists in several languages`() {
        val suggestions = search.suggest("scream", limit = 8)

        assertThat(suggestions.count { it.contains("Scream 4") }).isEqualTo(1)
        assertThat(suggestions.first()).isEqualTo("|FR| Scream 4 (2011)")
    }

    @Test
    fun `suggestions are capped`() {
        assertThat(search.suggest("s", limit = 3).size).isEqualTo(3)
    }

    @Test
    fun `a title starting with the whole word typed comes before one that only starts with those letters`() {
        val found = TitleSearch(listOf("Screamers", "Scream 4"), { it }).found("scream")

        assertThat(found).isEqualTo(listOf("Scream 4", "Screamers"))
    }

    @Test
    fun `a catalog of a hundred and fifty thousand titles is indexed and searched quickly`() {
        val words = listOf("Night", "Dream", "Scream", "Love", "War", "City", "Story", "Last", "Dark", "River", "Fire", "Star")
        val big = List(150_000) { "|FR| ${words[it % 12]} ${words[(it / 12) % 12]} ${it % 977} (${1980 + it % 45})" }

        val indexStart = System.nanoTime()
        val index = TitleSearch(big) { it }
        val indexMs = (System.nanoTime() - indexStart) / 1_000_000
        val searchStart = System.nanoTime()
        repeat(10) { index.suggest("scr", limit = 8) }
        val searchMs = (System.nanoTime() - searchStart) / 1_000_000 / 10

        // Generous bounds: a TV box is several times slower than the machine that runs this test.
        assertThat(indexMs < 3_000).isEqualTo(true)
        assertThat(searchMs < 150).isEqualTo(true)
        println("index $indexMs ms, one search $searchMs ms")
    }
}
