package com.androbito.tilfaz.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
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
    )

    @Test
    fun `sections list virtual entries then provider categories with counts`() = runTest {
        browser.snapshot.test {
            val sections = expectMostRecentItem().sections
            assertThat(sections.map { it.kind }.take(4)).containsExactly(
                CatalogBrowser.Section.Kind.ALL,
                CatalogBrowser.Section.Kind.FAVORITES,
                CatalogBrowser.Section.Kind.CONTINUE_WATCHING,
                CatalogBrowser.Section.Kind.RECENTLY_ADDED,
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
            assertThat(snapshot.progressById.keys).containsExactly(1)
        }
    }

    @Test
    fun `query filters inside the selected section`() = runTest {
        browser.select("11")
        browser.search("old")
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).isEqualTo(emptyList())
        }
        browser.select(CatalogBrowser.ALL)
        browser.snapshot.test {
            assertThat(expectMostRecentItem().items).containsExactly(older)
        }
    }
}
