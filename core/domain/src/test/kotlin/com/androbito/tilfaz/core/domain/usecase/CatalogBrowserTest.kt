package com.androbito.tilfaz.core.domain.usecase

import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CatalogBrowserTest {
    private val older = Fixtures.movie.copy(id = 1, name = "Old Film", categoryId = "10", addedAt = Fixtures.now.minusSeconds(86_400))
    private val newer = Fixtures.movie.copy(id = 2, name = "New Film", categoryId = "11", addedAt = Fixtures.now)
    private val vod = FakeVodRepository().apply {
        categories.value = listOf(Category("10", "Drama", ContentType.VOD), Category("11", "Action", ContentType.VOD))
        items.value = listOf(older, newer)
    }
    private val favorites = FakeFavoritesRepository()
    private val progress = FakeWatchProgressRepository()

    private val browser = CatalogBrowser(
        repository = vod,
        recent = vod::observeRecent,
        favoritesRepository = favorites,
        progressRepository = progress,
        contentType = ContentType.VOD,
        idOf = { it.id },
        nameOf = { it.name },
        favoriteKeyOf = ToggleFavoriteUseCase::vodKey,
        searchDelayMs = 0
    )

    // The sources never complete, and asSnapshot waits for the flow to, so only its first set of pages is taken.
    private suspend fun shown(): List<Movie> = browser.items.take(1).asSnapshot()

    @Test
    fun `sections list virtual entries then provider categories with counts`() = runTest {
        browser.snapshot.test {
            val sections = expectMostRecentItem().sections
            assertThat(sections.map { it.kind }.take(2)).containsExactly(
                CatalogBrowser.Section.Kind.ALL,
                CatalogBrowser.Section.Kind.RECENTLY_ADDED
            )
            // Favourites and Continue watching have a page of their own on Home.
            assertThat(sections.map { it.kind.name }).doesNotContain("FAVORITES")
            assertThat(sections.map { it.kind.name }).doesNotContain("CONTINUE_WATCHING")
            assertThat(sections.first().count).isEqualTo(2)
            assertThat(sections.first { it.name == "Drama" }.count).isEqualTo(1)
        }
    }

    @Test
    fun `the whole catalog is shown by default and a category shows only its own`() = runTest {
        assertThat(shown()).containsExactly(older, newer)

        browser.select("11")

        assertThat(shown()).containsExactly(newer)
    }

    @Test
    fun `recently added is sorted newest first`() = runTest {
        browser.select(CatalogBrowser.RECENTLY_ADDED)

        assertThat(shown()).containsExactly(newer, older)
    }

    @Test
    fun `saved progress still reaches the posters as a progress bar`() = runTest {
        progress.save(WatchProgress("vod:1", ContentType.VOD, "Old Film", null, 600_000, 6_000_000, Fixtures.now, "1"))

        browser.snapshot.test { assertThat(expectMostRecentItem().progressById.keys).containsOnly(1) }
    }

    @Test
    fun `a query searches the whole catalog whichever section is selected`() = runTest {
        browser.select("11")
        browser.search("old")

        assertThat(shown()).containsExactly(older)
    }

    @Test
    fun `clearing the query goes back to the selected section`() = runTest {
        browser.select("11")
        browser.search("old")
        browser.search("")

        assertThat(shown()).containsExactly(newer)
    }

    @Test
    fun `the search text is shown as typed`() = runTest {
        browser.search("ol")

        browser.snapshot.test { assertThat(expectMostRecentItem().query).isEqualTo("ol") }
    }

    @Test
    fun `search results are capped so a short query cannot flood the screen`() = runTest {
        vod.items.value = List(400) { Fixtures.movie.copy(id = 100 + it, name = "Film number $it", categoryId = "10") }
        browser.search("film")

        assertThat(shown().size).isEqualTo(CatalogBrowser.SEARCH_RESULT_LIMIT)
    }

    @Test
    fun `suggestions offer each title once`() = runTest {
        val french = Fixtures.movie.copy(id = 6, name = "|FR| Scream 4 (2011)", categoryId = "10")
        val english = Fixtures.movie.copy(id = 7, name = "|EN| Scream 4 (2011)", categoryId = "10")
        val other = Fixtures.movie.copy(id = 8, name = "|EN| Screamers", categoryId = "10")
        vod.items.value = listOf(french, english, other)
        browser.search("scr")

        browser.snapshot.test { assertThat(expectMostRecentItem().suggestions).containsExactly(french, other) }
    }

    @Test
    fun `without a query there are no suggestions`() = runTest {
        browser.snapshot.test { assertThat(expectMostRecentItem().suggestions).isEqualTo(emptyList()) }
    }

    @Test
    fun `at most eight suggestions are offered`() = runTest {
        vod.items.value = List(20) { Fixtures.movie.copy(id = 100 + it, name = "Film number $it", categoryId = "10") }
        browser.search("film")

        browser.snapshot.test { assertThat(expectMostRecentItem().suggestions.size).isEqualTo(CatalogBrowser.SUGGESTION_LIMIT) }
    }

    @Test
    fun `the snapshot says which section is selected`() = runTest {
        browser.select("10")

        browser.snapshot.test { assertThat(expectMostRecentItem().selectedSection).isNotNull() }
    }

    @Test
    fun `opening the screen does not wait for the typing pause`() = runTest {
        val realDelay = CatalogBrowser(
            repository = vod,
            recent = vod::observeRecent,
            favoritesRepository = favorites,
            progressRepository = progress,
            contentType = ContentType.VOD,
            idOf = { it.id },
            nameOf = { it.name },
            favoriteKeyOf = ToggleFavoriteUseCase::vodKey,
            searchDelayMs = 200
        )

        realDelay.snapshot.first()
        realDelay.items.first()

        assertThat(currentTime).isEqualTo(0L)
    }

    @Test
    fun `clearing the search does not wait either, only typing does`() = runTest {
        val realDelay = CatalogBrowser(
            repository = vod,
            recent = vod::observeRecent,
            favoritesRepository = favorites,
            progressRepository = progress,
            contentType = ContentType.VOD,
            idOf = { it.id },
            nameOf = { it.name },
            favoriteKeyOf = ToggleFavoriteUseCase::vodKey,
            searchDelayMs = 200
        )
        realDelay.search("old")
        realDelay.search("")

        realDelay.snapshot.first()

        assertThat(currentTime).isEqualTo(0L)
    }
}
