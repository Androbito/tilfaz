package com.androbito.tilfaz.feature.library

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val live = FakeLiveRepository().apply { items.value = Fixtures.liveChannels }
    private val vod = FakeVodRepository()
    private val series = FakeSeriesRepository().apply { items.value = listOf(Fixtures.series) }
    private val favorites = FakeFavoritesRepository()
    private val toggle = ToggleFavoriteUseCase(favorites)

    private val zebra = Fixtures.movie.copy(id = 601, name = "Zebra")
    private val apple = Fixtures.movie.copy(id = 602, name = "apple")
    private val other = Fixtures.movie.copy(id = 603, name = "Not a favourite")

    private fun viewModel() = FavoritesViewModel(live, vod, series, favorites, toggle)

    @Test
    fun `each section lists only its favourites, sorted by name`() = runTest {
        vod.items.value = listOf(zebra, apple, other)
        toggle.movie(zebra.id)
        toggle.movie(apple.id)
        toggle.live(Fixtures.alJazeera.id)
        toggle.series(Fixtures.series.id)

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertThat(state.channels).containsExactly(Fixtures.alJazeera)
            assertThat(state.movies).containsExactly(apple, zebra)
            assertThat(state.series).containsExactly(Fixtures.series)
        }
    }

    @Test
    fun `with no favourites every section is empty`() = runTest {
        viewModel().state.test {
            val state = expectMostRecentItem()
            assertThat(state.channels).isEmpty()
            assertThat(state.movies).isEmpty()
            assertThat(state.series).isEmpty()
        }
    }

    @Test
    fun `asking to remove only proposes it and cancelling keeps the favourite`() = runTest {
        vod.items.value = listOf(zebra)
        toggle.movie(zebra.id)
        val vm = viewModel()
        vm.state.test {
            vm.onEvent(FavoritesEvent.RequestRemoval(ContentType.VOD, zebra.id, zebra.name))
            val asked = expectMostRecentItem()
            assertThat(asked.pendingRemoval).isEqualTo(PendingRemoval(ContentType.VOD, zebra.id, "Zebra"))
            assertThat(asked.movies).containsExactly(zebra)

            vm.onEvent(FavoritesEvent.CancelRemoval)
            val cancelled = expectMostRecentItem()
            assertThat(cancelled.pendingRemoval).isNull()
            assertThat(cancelled.movies).containsExactly(zebra)
        }
    }

    @Test
    fun `confirming removes the favourite and closes the question`() = runTest {
        vod.items.value = listOf(zebra, apple)
        toggle.movie(zebra.id)
        toggle.movie(apple.id)
        val vm = viewModel()
        vm.state.test {
            vm.onEvent(FavoritesEvent.RequestRemoval(ContentType.VOD, zebra.id, zebra.name))
            vm.onEvent(FavoritesEvent.ConfirmRemoval)

            val state = expectMostRecentItem()
            assertThat(state.movies).containsExactly(apple)
            assertThat(state.pendingRemoval).isNull()
        }
    }

    @Test
    fun `each type is removed through its own key`() = runTest {
        toggle.live(Fixtures.alJazeera.id)
        toggle.series(Fixtures.series.id)
        val vm = viewModel()
        vm.state.test {
            vm.onEvent(FavoritesEvent.RequestRemoval(ContentType.LIVE, Fixtures.alJazeera.id, "x"))
            vm.onEvent(FavoritesEvent.ConfirmRemoval)
            vm.onEvent(FavoritesEvent.RequestRemoval(ContentType.SERIES, Fixtures.series.id, "y"))
            vm.onEvent(FavoritesEvent.ConfirmRemoval)

            val state = expectMostRecentItem()
            assertThat(state.channels).isEmpty()
            assertThat(state.series).isEmpty()
        }
    }
}
