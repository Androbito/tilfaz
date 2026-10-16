package com.androbito.tilfaz.feature.series

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SeriesDetailsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = FakeSeriesRepository().apply { items.value = listOf(Fixtures.series) }
    private val favorites = FakeFavoritesRepository()
    private val progress = FakeWatchProgressRepository()

    private fun viewModel() = SeriesDetailsViewModel(
        SavedStateHandle(mapOf("seriesId" to Fixtures.series.id)),
        repository,
        favorites,
        progress,
        ToggleFavoriteUseCase(favorites),
    )

    @Test
    fun `first season is selected and its episodes listed`() = runTest {
        repository.detailsResult = { Result.success(Fixtures.seriesDetails) }
        viewModel().state.test {
            val state = expectMostRecentItem()
            assertThat(state.selectedSeason).isEqualTo(1)
            assertThat(state.episodes.size).isEqualTo(2)
            assertThat(state.nextEpisode?.id).isEqualTo("90101")
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `play button targets the episode in progress`() = runTest {
        repository.detailsResult = { Result.success(Fixtures.seriesDetails) }
        progress.save(WatchProgress("series:901:90102", ContentType.SERIES, "x", null, 600_000, 2_580_000, Fixtures.now, "901:90102"))
        viewModel().state.test {
            val state = expectMostRecentItem()
            assertThat(state.nextEpisode?.id).isEqualTo("90102")
            assertThat(state.progressByEpisodeId.keys).isEqualTo(setOf("90102"))
        }
    }

    @Test
    fun `failure exposes retry and retry recovers`() = runTest {
        var attempts = 0
        repository.detailsResult = { if (attempts++ == 0) Result.failure(DomainException(DomainError.Network)) else Result.success(Fixtures.seriesDetails) }
        val vm = viewModel()
        vm.state.test {
            assertThat(expectMostRecentItem().failed).isTrue()
            vm.onEvent(SeriesDetailsEvent.Retry)
            val recovered = expectMostRecentItem()
            assertThat(recovered.failed).isFalse()
            assertThat(recovered.details).isEqualTo(Fixtures.seriesDetails)
        }
    }
}
