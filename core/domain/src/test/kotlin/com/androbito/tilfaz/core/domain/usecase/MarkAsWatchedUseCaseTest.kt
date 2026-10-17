package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MarkAsWatchedUseCaseTest {
    private val repository = FakeWatchProgressRepository()
    private val markAsWatched = MarkAsWatchedUseCase(repository)

    private val halfWatched = WatchProgress("vod:501", ContentType.VOD, "Movie", "http://img/p.jpg", 600_000, 1_200_000, Fixtures.now, "501")

    @Test
    fun `an item is finished once it is marked as watched`() = runTest {
        repository.save(halfWatched)

        markAsWatched(halfWatched)

        val saved = repository.progress("vod:501")!!
        assertThat(saved.isFinished).isTrue()
        assertThat(saved.positionMs).isEqualTo(saved.durationMs)
    }

    @Test
    fun `it keeps what identifies the item`() = runTest {
        markAsWatched(halfWatched)

        val saved = repository.progress("vod:501")!!
        assertThat(saved.title).isEqualTo("Movie")
        assertThat(saved.reference).isEqualTo("501")
    }

    @Test
    fun `an item that never reported a duration cannot be finished and is left alone`() = runTest {
        val unknownLength = halfWatched.copy(durationMs = 0)
        repository.save(unknownLength)

        markAsWatched(unknownLength)

        assertThat(repository.progress("vod:501")).isEqualTo(unknownLength)
    }
}
