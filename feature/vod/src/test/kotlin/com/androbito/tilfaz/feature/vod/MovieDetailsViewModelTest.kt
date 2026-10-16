package com.androbito.tilfaz.feature.vod

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MovieDetailsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val vod = FakeVodRepository().apply { items.value = listOf(Fixtures.movie) }
    private val favorites = FakeFavoritesRepository()
    private val progress = FakeWatchProgressRepository()
    private val details = MovieDetails(Fixtures.movie.copy(containerExtension = "mp4"), "Plot", "Cast", "Director", "Drama", "2026-01-01", 6000, emptyList(), null)

    private fun viewModel() = MovieDetailsViewModel(
        SavedStateHandle(mapOf("movieId" to Fixtures.movie.id)),
        vod,
        favorites,
        progress,
        ToggleFavoriteUseCase(favorites),
    )

    @Test
    fun `details replace the list row when they load`() = runTest {
        vod.detailsResult = { Result.success(details) }
        viewModel().state.test {
            val state = expectMostRecentItem()
            assertThat(state.movie?.containerExtension).isEqualTo("mp4")
            assertThat(state.details?.plot).isEqualTo("Plot")
            assertThat(state.detailsFailed).isFalse()
        }
    }

    @Test
    fun `failed details still leave the movie playable`() = runTest {
        vod.detailsResult = { Result.failure(DomainException(DomainError.Network)) }
        viewModel().state.test {
            val state = expectMostRecentItem()
            assertThat(state.movie).isNotNull()
            assertThat(state.details).isNull()
            assertThat(state.detailsFailed).isTrue()
        }
    }

    @Test
    fun `resume is offered when progress exists and start over clears it`() = runTest {
        vod.detailsResult = { Result.success(details) }
        progress.save(WatchProgress("vod:501", ContentType.VOD, "x", null, 900_000, 6_000_000, Fixtures.now, "501"))
        val vm = viewModel()
        vm.state.test {
            assertThat(expectMostRecentItem().canResume).isTrue()
            vm.onEvent(MovieDetailsEvent.StartOver)
            assertThat(expectMostRecentItem().canResume).isFalse()
        }
    }

    @Test
    fun `favourite toggles are reflected`() = runTest {
        vod.detailsResult = { Result.success(details) }
        val vm = viewModel()
        vm.state.test {
            assertThat(expectMostRecentItem().isFavorite).isFalse()
            vm.onEvent(MovieDetailsEvent.ToggleFavorite)
            assertThat(expectMostRecentItem().isFavorite).isTrue()
        }
    }
}
