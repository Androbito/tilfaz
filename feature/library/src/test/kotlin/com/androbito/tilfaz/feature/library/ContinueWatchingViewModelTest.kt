package com.androbito.tilfaz.feature.library

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.domain.usecase.MarkAsWatchedUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ContinueWatchingViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val progress = FakeWatchProgressRepository()
    private val vod = FakeVodRepository()
    private val series = FakeSeriesRepository().apply { items.value = listOf(Fixtures.series) }

    private val first = Fixtures.movie.copy(id = 601, name = "First")
    private val second = Fixtures.movie.copy(id = 602, name = "Second")

    private fun viewModel() = ContinueWatchingViewModel(progress, vod, series, MarkAsWatchedUseCase(progress))

    private fun movieProgress(movieId: Int, positionMs: Long = 600_000, updatedSeconds: Long = 0) = WatchProgress(
        "vod:$movieId",
        ContentType.VOD,
        "Movie $movieId",
        null,
        positionMs,
        1_200_000,
        Fixtures.now.plusSeconds(updatedSeconds),
        "$movieId"
    )

    private fun episodeProgress(episodeId: String, updatedSeconds: Long = 0) = WatchProgress(
        "series:901:$episodeId",
        ContentType.SERIES,
        "Episode",
        null,
        600_000,
        1_200_000,
        Fixtures.now.plusSeconds(updatedSeconds),
        "901:$episodeId"
    )

    @Test
    fun `a movie in progress is listed with its progress`() = runTest {
        vod.items.value = listOf(first)
        progress.save(movieProgress(601))

        viewModel().state.test {
            val movie = expectMostRecentItem().movies.single()
            assertThat(movie.movie).isEqualTo(first)
            assertThat(movie.progress.positionMs).isEqualTo(600_000)
        }
    }

    @Test
    fun `the most recently watched movie comes first`() = runTest {
        vod.items.value = listOf(first, second)
        progress.save(movieProgress(601, updatedSeconds = 10))
        progress.save(movieProgress(602, updatedSeconds = 99))

        viewModel().state.test {
            assertThat(expectMostRecentItem().movies.map { it.movie.id }).containsExactly(602, 601)
        }
    }

    @Test
    fun `a finished movie is not listed`() = runTest {
        vod.items.value = listOf(first)
        progress.save(movieProgress(601, positionMs = 1_190_000))

        viewModel().state.test { assertThat(expectMostRecentItem().movies).isEmpty() }
    }

    @Test
    fun `progress for a movie that is no longer in the catalog is skipped`() = runTest {
        vod.items.value = listOf(first)
        progress.save(movieProgress(601))
        progress.save(movieProgress(999))

        viewModel().state.test {
            assertThat(expectMostRecentItem().movies.map { it.movie.id }).containsExactly(601)
        }
    }

    @Test
    fun `a show appears once, at the episode watched last`() = runTest {
        progress.save(episodeProgress("90101", updatedSeconds = 10))
        progress.save(episodeProgress("90102", updatedSeconds = 50))

        viewModel().state.test {
            val show = expectMostRecentItem().shows.single()
            assertThat(show.series).isEqualTo(Fixtures.series)
            assertThat(show.episodeId).isEqualTo("90102")
        }
    }

    @Test
    fun `asking only proposes it and cancelling keeps the item`() = runTest {
        vod.items.value = listOf(first)
        progress.save(movieProgress(601))
        val vm = viewModel()
        vm.state.test {
            val item = expectMostRecentItem().movies.single()
            vm.onEvent(ContinueWatchingEvent.RequestMarkWatched(item.progress, "First"))
            assertThat(expectMostRecentItem().pendingWatched?.name).isEqualTo("First")

            vm.onEvent(ContinueWatchingEvent.CancelMarkWatched)
            val state = expectMostRecentItem()
            assertThat(state.pendingWatched).isNull()
            assertThat(state.movies.map { it.movie.id }).containsExactly(601)
        }
    }

    @Test
    fun `confirming marks it watched, so it leaves the list`() = runTest {
        vod.items.value = listOf(first, second)
        progress.save(movieProgress(601, updatedSeconds = 1))
        progress.save(movieProgress(602, updatedSeconds = 2))
        val vm = viewModel()
        vm.state.test {
            val target = expectMostRecentItem().movies.first { it.movie.id == 601 }
            vm.onEvent(ContinueWatchingEvent.RequestMarkWatched(target.progress, "First"))
            vm.onEvent(ContinueWatchingEvent.ConfirmMarkWatched)

            val state = expectMostRecentItem()
            assertThat(state.movies.map { it.movie.id }).containsExactly(602)
            assertThat(state.pendingWatched).isNull()
        }
    }
}
