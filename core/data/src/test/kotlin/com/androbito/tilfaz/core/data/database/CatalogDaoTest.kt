package com.androbito.tilfaz.core.data.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.database.CatalogSearch
import com.androbito.tilfaz.core.database.TilfazDatabase
import com.androbito.tilfaz.core.database.entity.LiveChannelEntity
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.database.entity.SeriesEntity
import com.androbito.tilfaz.core.model.TitleText
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CatalogDaoTest {
    private lateinit var database: TilfazDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TilfazDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    private fun movie(id: Int, name: String, category: String? = "10", position: Int = id, added: Long? = null) = MovieEntity(
        id = id, name = name, nameNormalized = TitleText.searchKey(name), posterUrl = null, categoryId = category,
        ratingOutOfTen = null, year = null, addedAtMillis = added, containerExtension = "mkv", position = position
    )

    private suspend fun givenMovies(vararg movies: MovieEntity) = database.movieDao().insertAll(movies.toList())

    private fun searchNames(query: String, limit: Int = 50): List<String> =
        database.openHelper.readableDatabase.query(CatalogSearch.query("movies", TitleText.normalize(query), limit)).use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            buildList { while (cursor.moveToNext()) add(cursor.getString(name)) }
        }

    // ---- search: the same ranking as the in-memory engine it replaces

    @Test
    fun `search puts a title that starts with the whole word before one with a word starting that way, then inside a word`() = runTest {
        givenMovies(movie(1, "Upstream"), movie(2, "The Stream"), movie(3, "Stream Team"))

        assertThat(searchNames("stream")).containsExactly("Stream Team", "The Stream", "Upstream")
    }

    @Test
    fun `search finds the first letters of any word, in any order`() = runTest {
        givenMovies(movie(1, "|FR| La Vénus électrique (2026)"), movie(2, "|FR| Backrooms (2026)"))

        assertThat(searchNames("elec venus")).containsExactly("|FR| La Vénus électrique (2026)")
        assertThat(searchNames("SPE").size).isEqualTo(0)
    }

    @Test
    fun `search ignores the provider tag and accents`() = runTest {
        givenMovies(movie(1, "|FR| Les Spécialistes (2026)"))

        assertThat(searchNames("specialistes")).containsExactly("|FR| Les Spécialistes (2026)")
        assertThat(searchNames("fr")).isEmpty()
    }

    @Test
    fun `a heading entry is never found`() = runTest {
        givenMovies(movie(1, "-----| BEIN SPORTS |-----"), movie(2, "BEIN SPORTS 1"))

        assertThat(searchNames("bein")).containsExactly("BEIN SPORTS 1")
    }

    @Test
    fun `equal matches keep the catalog order and the result count is capped`() = runTest {
        givenMovies(movie(1, "Scream 4 (2011)", position = 2), movie(2, "Scream 4 (2011)", position = 0), movie(3, "Scream 4 (2011)", position = 1))

        assertThat(searchNames("scream 4").size).isEqualTo(3)
        assertThat(database.openHelper.readableDatabase.query(CatalogSearch.query("movies", "scream 4", 2)).use { it.count }).isEqualTo(2)
    }

    @Test
    fun `the arabic title is found by letters that differ only by hamza or a final form`() = runTest {
        givenMovies(movie(1, "| The Mysterious Sound! | 2026 | أحمد مدرسة"))

        assertThat(searchNames("احمد مدرسه").size).isEqualTo(1)
    }

    // ---- counts

    @Test
    fun `counts per category come from the database, not from loading the rows`() = runTest {
        givenMovies(movie(1, "A", "10"), movie(2, "B", "10"), movie(3, "C", "11"), movie(4, "D", null))

        database.movieDao().observeCategoryCounts().test {
            assertThat(awaitItem().associate { it.categoryId to it.count }).isEqualTo(mapOf<String?, Int>("10" to 2, "11" to 1, null to 1))
        }
        database.movieDao().observeCount().test { assertThat(awaitItem()).isEqualTo(4) }
    }

    // ---- paging

    private suspend fun PagingSource<Int, MovieEntity>.firstPage(size: Int): List<String> {
        val result = load(PagingSource.LoadParams.Refresh(key = null, loadSize = size, placeholdersEnabled = false))
        return (result as PagingSource.LoadResult.Page).data.map { it.name }
    }

    @Test
    fun `a page of the whole catalog follows the provider's order`() = runTest {
        givenMovies(movie(1, "third", position = 2), movie(2, "first", position = 0), movie(3, "second", position = 1))

        assertThat(database.movieDao().pagingAll().firstPage(2)).containsExactly("first", "second")
    }

    @Test
    fun `a page of one category holds only that category`() = runTest {
        givenMovies(movie(1, "in", "10", position = 0), movie(2, "out", "11", position = 1), movie(3, "also in", "10", position = 2))

        assertThat(database.movieDao().pagingByCategory("10").firstPage(10)).containsExactly("in", "also in")
    }

    // ---- bounded reads

    @Test
    fun `related titles are the first few of the same category without the movie itself`() = runTest {
        givenMovies(movie(1, "me", "10", position = 0), movie(2, "a", "10", position = 1), movie(3, "b", "10", position = 2), movie(4, "other", "11", position = 3))

        database.movieDao().observeRelated("10", excludeId = 1, limit = 1).test {
            assertThat(awaitItem().map { it.name }).containsExactly("a")
        }
    }

    @Test
    fun `recently added is the newest few`() = runTest {
        givenMovies(movie(1, "old", added = 1), movie(2, "new", added = 3), movie(3, "mid", added = 2))

        database.movieDao().observeRecentlyAdded(2).test {
            assertThat(awaitItem().map { it.name }).containsExactly("new", "mid")
        }
    }

    // ---- the other two catalogs use the same machinery

    @Test
    fun `series and channels are counted and searched the same way`() = runTest {
        database.seriesDao().insertAll(
            listOf(
                SeriesEntity(1, "Breaking Point", TitleText.searchKey("Breaking Point"), null, "5", null, null, null, null, null, 0)
            )
        )
        database.liveChannelDao().insertAll(
            listOf(
                LiveChannelEntity(1, 1, "|AR| AL JAZEERA HD", TitleText.searchKey("|AR| AL JAZEERA HD"), null, "2", null, false, 0, 0)
            )
        )

        database.seriesDao().observeCount().test { assertThat(awaitItem()).isEqualTo(1) }
        database.liveChannelDao().observeCount().test { assertThat(awaitItem()).isEqualTo(1) }
        val db = database.openHelper.readableDatabase
        assertThat(db.query(CatalogSearch.query("series", "break", 10)).use { it.count }).isEqualTo(1)
        assertThat(db.query(CatalogSearch.query("live_channels", "jaz", 10)).use { it.count }).isEqualTo(1)
    }
}
