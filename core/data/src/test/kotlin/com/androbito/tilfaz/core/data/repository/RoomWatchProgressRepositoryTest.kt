package com.androbito.tilfaz.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import com.androbito.tilfaz.core.database.TilfazDatabase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RoomWatchProgressRepositoryTest {
    private lateinit var database: TilfazDatabase
    private lateinit var repository: RoomWatchProgressRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TilfazDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomWatchProgressRepository(database.watchProgressDao(), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = database.close()

    private fun progress(id: Int, positionMs: Long, updatedSeconds: Long) = WatchProgress(
        playableKey = "vod:$id",
        contentType = ContentType.VOD,
        title = "Movie $id",
        posterUrl = null,
        positionMs = positionMs,
        durationMs = 1_000_000,
        updatedAt = Instant.ofEpochSecond(updatedSeconds),
        reference = "$id"
    )

    @Test
    fun `finished movies do not use up the slots of the continue watching list`() = runTest {
        repository.save(progress(id = 1, positionMs = 300_000, updatedSeconds = 1))
        // 35 more recent movies that were watched to the end: more than the list can hold.
        (2..36).forEach { repository.save(progress(id = it, positionMs = 990_000, updatedSeconds = it.toLong())) }

        repository.observeContinueWatching(ContentType.VOD).test {
            assertThat(awaitItem().map { it.playableKey }).containsExactly("vod:1")
        }
    }
}
