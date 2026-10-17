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
        ToggleFavoriteUseCase(favorites)
    )

    private fun episodeProgress(episodeId: String, positionMs: Long, durationMs: Long = 2_580_000, updatedSecondsAfterNow: Long = 0) = WatchProgress(
        playableKey = "series:901:$episodeId",
        contentType = ContentType.SERIES,
        title = "x",
        posterUrl = null,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = Fixtures.now.plusSeconds(updatedSecondsAfterNow),
        reference = "901:$episodeId"
    )

    @Test
    fun `after a finished episode the big button offers the next one`() = runTest {
        repository.detailsResult = { Result.success(Fixtures.seriesDetails) }
        progress.save(episodeProgress("90101", positionMs = 2_500_000))

        viewModel().state.test {
            assertThat(expectMostRecentItem().nextEpisode?.id).isEqualTo("90102")
        }
    }

    @Test
    fun `when the last episode is finished the big button starts over`() = runTest {
        repository.detailsResult = { Result.success(Fixtures.seriesDetails) }
        progress.save(episodeProgress("90101", positionMs = 2_500_000))
        progress.save(episodeProgress("90102", positionMs = 2_500_000, updatedSecondsAfterNow = 60))

        viewModel().state.test {
            assertThat(expectMostRecentItem().nextEpisode?.id).isEqualTo("90101")
        }
    }

    @Test
    fun `an episode in progress wins over a later unwatched one`() = runTest {
        repository.detailsResult = { Result.success(Fixtures.seriesDetails) }
        progress.save(episodeProgress("90101", positionMs = 600_000))

        viewModel().state.test {
            assertThat(expectMostRecentItem().nextEpisode?.id).isEqualTo("90101")
        }
    }

    @Test
    fun `related series come from the same category and exclude the series itself`() = runTest {
        val sameCategory = Fixtures.series.copy(id = 902, name = "Same category")
        val otherCategory = Fixtures.series.copy(id = 903, name = "Other", categoryId = "99")
        repository.items.value = listOf(Fixtures.series, sameCategory, otherCategory)
        repository.detailsResult = { Result.success(Fixtures.seriesDetails) }

        viewModel().state.test {
            assertThat(expectMostRecentItem().related).isEqualTo(listOf(sameCategory))
        }
    }

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
