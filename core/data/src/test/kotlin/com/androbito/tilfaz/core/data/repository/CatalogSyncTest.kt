package com.androbito.tilfaz.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import com.androbito.tilfaz.core.data.store.AccountStore
import com.androbito.tilfaz.core.data.store.SecretCodec
import com.androbito.tilfaz.core.database.TilfazDatabase
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.network.XtreamApi
import com.androbito.tilfaz.core.network.dto.CategoryDto
import java.io.File
import java.lang.reflect.Proxy
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CatalogSyncTest {
    private lateinit var database: TilfazDatabase
    private lateinit var context: Context
    private val responses = mutableMapOf<String, () -> Any?>()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val api = Proxy.newProxyInstance(XtreamApi::class.java.classLoader, arrayOf(XtreamApi::class.java)) { _, method, _ ->
        (responses[method.name] ?: error("unexpected call: ${method.name}")).invoke()
    } as XtreamApi

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TilfazDatabase::class.java).allowMainThreadQueries().build()
        responses["vodCategories"] = { listOf(CategoryDto(categoryId = "10", categoryName = "Drama")) }
        responses["seriesCategories"] = { listOf(CategoryDto(categoryId = "20", categoryName = "Crime")) }
        responses["liveCategories"] = { listOf(CategoryDto(categoryId = "30", categoryName = "News")) }
    }

    @After
    fun tearDown() = database.close()

    private fun vod() = DefaultVodRepository(
        api,
        database.categoryDao(),
        database.movieDao(),
        json,
        database.syncStatusDao(),
        Clock.systemUTC(),
        Dispatchers.Unconfined
    )

    private fun series() = DefaultSeriesRepository(
        api,
        database.categoryDao(),
        database.seriesDao(),
        json,
        database.syncStatusDao(),
        Clock.systemUTC(),
        Dispatchers.Unconfined
    )

    private fun live() = DefaultLiveRepository(
        api,
        database.categoryDao(),
        database.liveChannelDao(),
        AccountStore(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.Unconfined), produceFile = {
                File(context.cacheDir, "sync-${System.nanoTime()}.preferences_pb")
            }),
            object : SecretCodec {
                override fun encrypt(plain: String) = plain

                override fun decrypt(encoded: String) = encoded
            }
        ),
        json,
        database.syncStatusDao(),
        Clock.systemUTC(),
        Dispatchers.Unconfined
    )

    private fun body(text: String): ResponseBody = text.toResponseBody("application/json".toMediaType())

    private fun movieJson(count: Int, startId: Int = 1, skipEvery: Int = 0) = (0 until count).joinToString(",", "[", "]") {
        if (skipEvery > 0 &&
            it % skipEvery == 0
        ) {
            """{"name":"no id $it"}"""
        } else {
            """{"stream_id":"${startId + it}","name":"|FR| Movie ${startId + it}","category_id":"10","stream_icon":"http://img/${startId + it}.jpg"}"""
        }
    }

    private suspend fun storedMovies() = database.movieDao().observeRecentlyAdded(Int.MAX_VALUE).first()

    @Test
    fun `a catalog larger than one chunk is stored whole and in the provider's order`() = runTest {
        responses["vodStreams"] = { body(movieJson(1_300)) }

        assertThat(vod().refresh()).isSuccess()

        database.movieDao().observeCount().test { assertThat(awaitItem()).isEqualTo(1_300) }
        val source = database.movieDao().pagingAll().load(androidx.paging.PagingSource.LoadParams.Refresh(null, 5, false))
        val first = (source as androidx.paging.PagingSource.LoadResult.Page).data
        assertThat(first.map { it.id }).isEqualTo(listOf(1, 2, 3, 4, 5))
        assertThat(first.map { it.position }).isEqualTo(listOf(0, 1, 2, 3, 4))
        assertThat(first.first().nameNormalized).isEqualTo("movie 1")
        assertThat(first.first().posterUrl).isEqualTo("http://img/1.jpg")
    }

    @Test
    fun `positions follow the whole list, also across the chunks and past rows that had no id`() = runTest {
        responses["vodStreams"] = { body(movieJson(1_100, skipEvery = 100)) }

        vod().refresh()

        val rows = database.movieDao().pagingAll().load(androidx.paging.PagingSource.LoadParams.Refresh(null, 2_000, false))
        val data = (rows as androidx.paging.PagingSource.LoadResult.Page).data
        assertThat(data.size).isEqualTo(1_089) // 1,100 minus the 11 without an id
        assertThat(data.map { it.position } == data.map { it.position }.sorted()).isEqualTo(true)
        assertThat(data.last().position).isEqualTo(1_099)
    }

    @Test
    fun `the new catalog replaces the old one`() = runTest {
        database.movieDao().insertAll(listOf(MovieEntity(999, "Gone", TitleText.searchKey("Gone"), null, "10", null, null, null, "mkv", 0)))
        responses["vodStreams"] = { body(movieJson(3)) }

        vod().refresh()

        assertThat(database.movieDao().byId(999)).isNull()
        assertThat(database.movieDao().byId(2)).isNotNull()
    }

    @Test
    fun `a download that breaks halfway leaves the old catalog untouched and says it failed`() = runTest {
        database.movieDao().insertAll(listOf(MovieEntity(999, "Kept", TitleText.searchKey("Kept"), null, "10", null, null, null, "mkv", 0)))
        val broken = movieJson(800).dropLast(40) + """{"stream_id": !!! broken"""
        responses["vodStreams"] = { body(broken) }

        val result = vod().refresh()

        assertThat(result).isFailure()
        assertThat(database.movieDao().byId(999)).isNotNull()
        assertThat(database.movieDao().byId(1)).isNull()
    }

    @Test
    fun `a failed sync is not recorded as a sync`() = runTest {
        responses["vodStreams"] = { body("""{"user_info":{"auth":0}}""") }
        val repository = vod()

        repository.refresh()

        assertThat(repository.observeSyncStatus().first().lastSyncedAt).isNull()
    }

    @Test
    fun `a good sync records when it happened and how many items it stored`() = runTest {
        responses["vodStreams"] = { body(movieJson(7)) }
        val repository = vod()

        repository.refresh()

        val status = repository.observeSyncStatus().first()
        assertThat(status.itemCount).isEqualTo(7)
        assertThat(status.lastSyncedAt).isNotNull()
    }

    @Test
    fun `categories are stored with the catalog`() = runTest {
        responses["vodStreams"] = { body(movieJson(1)) }
        val repository = vod()

        repository.refresh()

        assertThat(repository.observeCategories().first().map { it.name }).isEqualTo(listOf("Drama"))
    }

    @Test
    fun `series keep a short plot as the fallback for a details page, not the whole text`() = runTest {
        responses["series"] = { body("""[{"series_id":"1","name":"Show","category_id":"20","plot":"${"x".repeat(5_000)}","genre":"${"g".repeat(500)}"}]""") }

        series().refresh()

        val stored = database.seriesDao().byId(1)!!
        assertThat(stored.plot!!.length).isEqualTo(400)
        assertThat(stored.genre!!.length).isEqualTo(100)
    }

    @Test
    fun `live channels are streamed in the same way`() = runTest {
        responses["liveStreams"] =
            { body("""[{"stream_id":"1","name":"|AR| AL JAZEERA HD","category_id":"30"},{"stream_id":"2","name":"-----| X |-----","category_id":"30"}]""") }

        assertThat(live().refresh()).isSuccess()

        assertThat(database.liveChannelDao().byId(1)!!.nameNormalized).isEqualTo("al jazeera hd")
        assertThat(database.liveChannelDao().byId(2)!!.nameNormalized).isEqualTo("")
        assertThat(ContentType.LIVE.name).isEqualTo("LIVE")
    }
}
