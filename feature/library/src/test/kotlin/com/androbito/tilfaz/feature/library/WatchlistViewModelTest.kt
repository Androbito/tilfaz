package com.androbito.tilfaz.feature.library

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.domain.usecase.ToggleWatchlistUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchlistRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class WatchlistViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val vod = FakeVodRepository()
    private val series = FakeSeriesRepository().apply { items.value = listOf(Fixtures.series) }
    private val watchlist = FakeWatchlistRepository()
    private val toggle = ToggleWatchlistUseCase(watchlist)

    private val zebra = Fixtures.movie.copy(id = 601, name = "Zebra")
    private val apple = Fixtures.movie.copy(id = 602, name = "apple")
    private val other = Fixtures.movie.copy(id = 603, name = "Not planned")

    private fun viewModel() = WatchlistViewModel(vod, series, watchlist, toggle)

    @Test
    fun `each section lists only what is on the watch list, sorted by name`() = runTest {
        vod.items.value = listOf(zebra, apple, other)
        toggle.movie(zebra.id)
        toggle.movie(apple.id)
        toggle.series(Fixtures.series.id)

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertThat(state.movies).containsExactly(apple, zebra)
            assertThat(state.series).containsExactly(Fixtures.series)
        }
    }

    @Test
    fun `favourites are not on the watch list`() = runTest {
        vod.items.value = listOf(zebra)
        ToggleFavoriteUseCase(FakeFavoritesRepository()).movie(zebra.id)

        viewModel().state.test {
            assertThat(expectMostRecentItem().movies).isEmpty()
        }
    }

    @Test
    fun `asking to remove only proposes it and cancelling keeps the item`() = runTest {
        vod.items.value = listOf(zebra)
        toggle.movie(zebra.id)
        val vm = viewModel()
        vm.state.test {
            vm.onEvent(WatchlistEvent.RequestRemoval(ContentType.VOD, zebra.id, zebra.name))
            val asked = expectMostRecentItem()
            assertThat(asked.pendingRemoval).isEqualTo(PendingRemoval(ContentType.VOD, zebra.id, "Zebra"))
            assertThat(asked.movies).containsExactly(zebra)

            vm.onEvent(WatchlistEvent.CancelRemoval)
            val cancelled = expectMostRecentItem()
            assertThat(cancelled.pendingRemoval).isNull()
            assertThat(cancelled.movies).containsExactly(zebra)
        }
    }

    @Test
    fun `confirming takes it off the watch list and closes the question`() = runTest {
        vod.items.value = listOf(zebra, apple)
        toggle.movie(zebra.id)
        toggle.movie(apple.id)
        toggle.series(Fixtures.series.id)
        val vm = viewModel()
        vm.state.test {
            vm.onEvent(WatchlistEvent.RequestRemoval(ContentType.VOD, zebra.id, zebra.name))
            vm.onEvent(WatchlistEvent.ConfirmRemoval)
            val afterMovie = expectMostRecentItem()
            assertThat(afterMovie.movies).containsExactly(apple)
            assertThat(afterMovie.pendingRemoval).isNull()

            vm.onEvent(WatchlistEvent.RequestRemoval(ContentType.SERIES, Fixtures.series.id, Fixtures.series.name))
            vm.onEvent(WatchlistEvent.ConfirmRemoval)
            assertThat(expectMostRecentItem().series).isEmpty()
        }
    }
}
