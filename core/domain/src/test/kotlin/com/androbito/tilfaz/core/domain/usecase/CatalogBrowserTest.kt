package com.androbito.tilfaz.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.Dispatchers
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
        favoritesRepository = favorites,
        progressRepository = progress,
        contentType = ContentType.VOD,
        idOf = { it.id },
        nameOf = { it.name },
        categoryOf = { it.categoryId },
        addedAtMillisOf = { it.addedAt?.toEpochMilli() },
        favoriteKeyOf = ToggleFavoriteUseCase::vodKey,
        computation = Dispatchers.Unconfined
    )

    @Test
    fun `sections list virtual entries then provider categories with counts`() = runTest {
        browser.snapshot.test {
            val sections = expectMostRecentItem().sections
            assertThat(sections.map { it.kind }.take(4)).containsExactly(
                CatalogBrowser.Section.Kind.ALL,
                CatalogBrowser.Section.Kind.FAVORITES,
                CatalogBrowser.Section.Kind.CONTINUE_WATCHING,
                CatalogBrowser.Section.Kind.RECENTLY_ADDED
            )
            assertThat(sections.first().count).isEqualTo(2)
            assertThat(sections.first { it.name == "Drama" }.count).isEqualTo(1)
        }
    }

    @Test
    fun `recently added is sorted newest first`() = runTest {
        browser.select(CatalogBrowser.RECENTLY_ADDED)
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).containsExactly(newer, older)
        }
    }

    @Test
    fun `continue watching lists items with saved progress`() = runTest {
        progress.save(WatchProgress("vod:1", ContentType.VOD, "Old Film", null, 600_000, 6_000_000, Fixtures.now, "1"))
        browser.select(CatalogBrowser.CONTINUE_WATCHING)
        browser.snapshot.test {
            val snapshot = expectMostRecentItem()
            assertThat(snapshot.items).containsExactly(older)
            assertThat(snapshot.progressById.keys).containsOnly(1)
        }
    }

    @Test
    fun `a query searches the whole catalog whichever section is selected`() = runTest {
        browser.select("11")
        browser.search("old")
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).containsExactly(older)
        }
    }

    @Test
    fun `clearing the query goes back to the selected section`() = runTest {
        browser.select("11")
        browser.search("old")
        browser.search("")
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).containsExactly(newer)
        }
    }

    @Test
    fun `results come best match first`() = runTest {
        val upstream = Fixtures.movie.copy(id = 3, name = "Upstream", categoryId = "10")
        val stream = Fixtures.movie.copy(id = 4, name = "Stream Team", categoryId = "10")
        vod.items.value = listOf(upstream, stream)
        browser.search("stream")
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).containsExactly(stream, upstream)
        }
    }

    @Test
    fun `the provider's language tag is not matched and the title is found without it`() = runTest {
        val tagged = Fixtures.movie.copy(id = 5, name = "|FR| Scream 4 (2011)", categoryId = "10")
        vod.items.value = listOf(tagged)
        browser.search("scream")
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).containsExactly(tagged)
        }
        browser.search("fr")
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).isEqualTo(emptyList())
        }
    }

    @Test
    fun `suggestions offer each title once with the best matches first`() = runTest {
        val french = Fixtures.movie.copy(id = 6, name = "|FR| Scream 4 (2011)", categoryId = "10")
        val english = Fixtures.movie.copy(id = 7, name = "|EN| Scream 4 (2011)", categoryId = "10")
        val other = Fixtures.movie.copy(id = 8, name = "|EN| Screamers", categoryId = "10")
        vod.items.value = listOf(french, english, other)
        browser.search("scr")
        browser.snapshot.test {
            val snapshot = expectMostRecentItem()
            // Both only start with the typed letters, so the shorter title comes first; the French and English Scream 4 count once.
            assertThat(snapshot.suggestions).containsExactly(other, french)
            assertThat(snapshot.items.size).isEqualTo(3)
        }
    }

    @Test
    fun `without a query there are no suggestions`() = runTest {
        browser.snapshot.test {
            assertThat(expectMostRecentItem().suggestions).isEqualTo(emptyList())
        }
    }

    @Test
    fun `at most eight suggestions are offered`() = runTest {
        vod.items.value = List(20) { Fixtures.movie.copy(id = 100 + it, name = "Film number $it", categoryId = "10") }
        browser.search("film")
        browser.snapshot.test {
            val snapshot = expectMostRecentItem()
            assertThat(snapshot.suggestions.size).isEqualTo(CatalogBrowser.SUGGESTION_LIMIT)
            assertThat(snapshot.items.size).isEqualTo(20)
        }
    }
}
