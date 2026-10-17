package com.androbito.tilfaz.core.data.repository

import android.content.Context
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.database.TilfazDatabase
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.database.entity.SeriesEntity
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.network.XtreamApi
import java.lang.reflect.Proxy
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CatalogRepositoriesTest {
    private lateinit var database: TilfazDatabase
    private lateinit var vod: DefaultVodRepository
    private lateinit var series: DefaultSeriesRepository

    /** Browsing the cache must never reach the network. */
    private val offlineApi = Proxy.newProxyInstance(XtreamApi::class.java.classLoader, arrayOf(XtreamApi::class.java)) { _, method, _ ->
        error("the catalog was read from the network: ${method.name}")
    } as XtreamApi

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TilfazDatabase::class.java).allowMainThreadQueries().build()
        vod = DefaultVodRepository(offlineApi, database.categoryDao(), database.movieDao(), database.syncStatusDao(), Clock.systemUTC(), Dispatchers.Unconfined)
        series =
            DefaultSeriesRepository(
                offlineApi,
                database.categoryDao(),
                database.seriesDao(),
                Json,
                database.syncStatusDao(),
                Clock.systemUTC(),
                Dispatchers.Unconfined
            )
    }

    @After
    fun tearDown() = database.close()

    private fun movie(id: Int, name: String, category: String? = "10", position: Int = id, added: Long? = null) = MovieEntity(
        id = id, name = name, nameNormalized = TitleText.searchKey(name), posterUrl = "http://img/$id.jpg", categoryId = category,
        ratingOutOfTen = 7.0, year = 2011, addedAtMillis = added, containerExtension = "mkv", position = position
    )

    private suspend fun givenMovies(vararg movies: MovieEntity) = database.movieDao().insertAll(movies.toList())

    @Test
    fun `the catalog is paged in the provider's order and mapped to movies`() = runTest {
        givenMovies(movie(1, "third", position = 2), movie(2, "first", position = 0), movie(3, "second", position = 1))

        val page = vod.pagedItems(null).asSnapshot()

        assertThat(page.map { it.name }).containsExactly("first", "second", "third")
        assertThat(page.first().posterUrl).isEqualTo("http://img/2.jpg") // the image stays a link
    }

    @Test
    fun `a category pages only its own movies`() = runTest {
        givenMovies(movie(1, "in", "10"), movie(2, "out", "11"))

        assertThat(vod.pagedItems("10").asSnapshot().map { it.name }).containsExactly("in")
    }

    @Test
    fun `only a window of a large catalog is held at once`() = runTest {
        givenMovies(*Array(1_000) { movie(it + 1, "Movie ${it + 1}") })

        val page = vod.pagedItems(null).asSnapshot()

        assertThat(page.size < 1_000).isEqualTo(true)
        assertThat(page.first().name).isEqualTo("Movie 1")
    }

    @Test
    fun `counts come back per category and in total`() = runTest {
        givenMovies(movie(1, "A", "10"), movie(2, "B", "10"), movie(3, "C", null))

        vod.observeCategoryCounts().test { assertThat(awaitItem()).isEqualTo(mapOf<String?, Int>("10" to 2, null to 1)) }
        vod.observeCount().test { assertThat(awaitItem()).isEqualTo(3) }
    }

    @Test
    fun `a search returns ranked movies, nothing for a blank query, and no more than asked`() = runTest {
        givenMovies(movie(1, "Upstream"), movie(2, "Stream Team"), movie(3, "Streaming"))

        vod.search("stream", limit = 10).test { assertThat(awaitItem().map { it.name }).containsExactly("Stream Team", "Streaming", "Upstream") }
        vod.search("stream", limit = 1).test { assertThat(awaitItem().map { it.name }).containsExactly("Stream Team") }
        vod.search("   ", limit = 10).test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `recently added and related are bounded reads`() = runTest {
        givenMovies(movie(1, "old", added = 1), movie(2, "new", added = 3), movie(3, "mid", added = 2))

        vod.observeRecent(2).test { assertThat(awaitItem().map { it.name }).containsExactly("new", "mid") }
        vod.observeRelated("10", excludeId = 1, limit = 1).test { assertThat(awaitItem().map { it.name }).containsExactly("new") }
    }

    @Test
    fun `series work the same way`() = runTest {
        database.seriesDao().insertAll(
            listOf(
                SeriesEntity(1, "Breaking Point", TitleText.searchKey("Breaking Point"), null, "5", null, null, null, null, null, 0),
                SeriesEntity(2, "Other", TitleText.searchKey("Other"), null, "5", null, null, 5, null, null, 1)
            )
        )

        assertThat(series.pagedItems("5").asSnapshot().map { it.name }).containsExactly("Breaking Point", "Other")
        series.search("break", limit = 5).test { assertThat(awaitItem().map { it.name }).containsExactly("Breaking Point") }
        series.observeCount().test { assertThat(awaitItem()).isEqualTo(2) }
        series.observeRelated("5", excludeId = 1, limit = 3).test { assertThat(awaitItem().map { it.name }).containsExactly("Other") }
    }
}
