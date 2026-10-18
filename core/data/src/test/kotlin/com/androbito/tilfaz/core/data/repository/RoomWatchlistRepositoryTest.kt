package com.androbito.tilfaz.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import com.androbito.tilfaz.core.database.TilfazDatabase
import com.androbito.tilfaz.core.model.ContentType
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RoomWatchlistRepositoryTest {
    private lateinit var database: TilfazDatabase
    private lateinit var repository: RoomWatchlistRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TilfazDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomWatchlistRepository(database.watchlistDao(), Clock.systemUTC(), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `toggling adds an item and toggling again takes it off`() = runTest {
        repository.toggle("vod:1", ContentType.VOD)
        repository.observeKeys(ContentType.VOD).test { assertThat(awaitItem()).containsOnly("vod:1") }

        repository.toggle("vod:1", ContentType.VOD)
        repository.observeKeys(ContentType.VOD).test { assertThat(awaitItem()).isEmpty() }
    }

    @Test
    fun `movies and series are listed apart`() = runTest {
        repository.toggle("vod:1", ContentType.VOD)
        repository.toggle("series:2", ContentType.SERIES)

        repository.observeKeys(ContentType.VOD).test { assertThat(awaitItem()).containsOnly("vod:1") }
        repository.observeKeys(ContentType.SERIES).test { assertThat(awaitItem()).containsOnly("series:2") }
    }

    @Test
    fun `removing drops an item and does nothing for one that is not there`() = runTest {
        repository.toggle("vod:1", ContentType.VOD)

        repository.remove("vod:1")
        repository.remove("vod:99")

        repository.observeKeys(ContentType.VOD).test { assertThat(awaitItem()).isEmpty() }
    }

    @Test
    fun `the watch list is not the favourites`() = runTest {
        val favorites = RoomFavoritesRepository(database.favoriteDao(), Clock.systemUTC(), Dispatchers.Unconfined)
        repository.toggle("vod:1", ContentType.VOD)

        favorites.observeKeys(ContentType.VOD).test { assertThat(awaitItem()).isEmpty() }
    }
}
