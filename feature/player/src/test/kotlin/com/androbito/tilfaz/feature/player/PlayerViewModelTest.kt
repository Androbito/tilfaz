package com.androbito.tilfaz.feature.player

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.androbito.tilfaz.core.domain.usecase.BuildStreamSourceUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import com.androbito.tilfaz.core.model.SubtitleStyle
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fake.FakeAccountRepository
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakePlaybackSettingsRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchProgressRepository
import com.androbito.tilfaz.core.testing.fake.FakeWatchlistRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val account = FakeAccountRepository(Fixtures.account)
    private val settings = FakePlaybackSettingsRepository()
    private val live = FakeLiveRepository().apply { items.value = Fixtures.liveChannels }
    private val vod = FakeVodRepository().apply { items.value = listOf(Fixtures.movie) }
    private val series = FakeSeriesRepository().apply { items.value = listOf(Fixtures.series); detailsResult = { Result.success(Fixtures.seriesDetails) } }
    private val progress = FakeWatchProgressRepository()
    private val watchlist = FakeWatchlistRepository()

    private fun viewModel(route: PlayerRoute) = PlayerViewModel(
        SavedStateHandle(
            mapOf(
                PlayerRoute.ARG_KIND to route.kind,
                PlayerRoute.ARG_ITEM_ID to route.itemId,
                PlayerRoute.ARG_PARENT_ID to route.parentId,
                PlayerRoute.ARG_CATEGORY_ID to route.categoryId,
                PlayerRoute.ARG_START_POSITION to route.startPositionMs
            )
        ),
        live,
        vod,
        series,
        progress,
        watchlist,
        settings,
        BuildStreamSourceUseCase(account, settings),
        Fixtures.fixedClock
    )

    @Test
    fun `live channel resolves to a ts url with its EPG`() = runTest {
        live.epg[Fixtures.alJazeera.id] = Result.success(listOf(Fixtures.epg(101, "News Hour", -10, 60)))
        viewModel(PlayerRoute.live(Fixtures.alJazeera.id, categoryId = null)).state.test {
            val state = expectMostRecentItem()
            assertThat(state.source?.url).isEqualTo("http://tv.example.com:8080/live/ayoub/s3cret/101.ts")
            assertThat(state.nowNext.now?.title).isEqualTo("News Hour")
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `switching format rebuilds the url and persists the preference`() = runTest {
        val vm = viewModel(PlayerRoute.live(Fixtures.alJazeera.id, null))
        vm.state.test {
            vm.onEvent(PlayerEvent.SwitchFormat)
            val state = expectMostRecentItem()
            assertThat(state.source?.url).isEqualTo("http://tv.example.com:8080/live/ayoub/s3cret/101.m3u8")
            assertThat(settings.settings.value.liveFormat).isEqualTo(StreamFormat.HLS)
        }
    }

    @Test
    fun `zapping moves to the next channel and wraps around`() = runTest {
        val vm = viewModel(PlayerRoute.live(Fixtures.beinSports1.id, null))
        vm.state.test {
            vm.onEvent(PlayerEvent.ZapNext)
            assertThat(expectMostRecentItem().title).isEqualTo(Fixtures.alJazeera.name)
            vm.onEvent(PlayerEvent.ZapPrevious)
            assertThat(expectMostRecentItem().title).isEqualTo(Fixtures.beinSports1.name)
        }
    }

    @Test
    fun `zapping stays inside the category the channel was opened from`() = runTest {
        val sameCategory = Fixtures.alJazeera.copy(id = 103, name = "AL JAZEERA 2")
        live.items.value = Fixtures.liveChannels + sameCategory
        val vm = viewModel(PlayerRoute.live(Fixtures.alJazeera.id, categoryId = Fixtures.alJazeera.categoryId))
        vm.state.test {
            vm.onEvent(PlayerEvent.ZapNext)
            assertThat(expectMostRecentItem().title).isEqualTo("AL JAZEERA 2")
            vm.onEvent(PlayerEvent.ZapNext)
            assertThat(expectMostRecentItem().title).isEqualTo(Fixtures.alJazeera.name) // wrapped, skipping the other category
        }
    }

    @Test
    fun `a channel alone in its category stays on itself when zapping`() = runTest {
        val vm = viewModel(PlayerRoute.live(Fixtures.beinSports1.id, categoryId = Fixtures.beinSports1.categoryId))
        vm.state.test {
            vm.onEvent(PlayerEvent.ZapNext)
            assertThat(expectMostRecentItem().title).isEqualTo(Fixtures.beinSports1.name)
        }
    }

    @Test
    fun `movie resumes from saved progress and ticks save new progress`() = runTest {
        progress.save(WatchProgress("vod:501", ContentType.VOD, "x", null, 1_200_000, 6_000_000, Fixtures.now, "501"))
        val vm = viewModel(PlayerRoute.movie(Fixtures.movie.id, startPositionMs = 0))
        vm.state.test {
            val state = expectMostRecentItem()
            assertThat(state.source?.url).isEqualTo("http://tv.example.com:8080/movie/ayoub/s3cret/501.mkv")
            assertThat(state.startPositionMs).isEqualTo(1_200_000L)
        }
        vm.onEvent(PlayerEvent.ProgressTick(1_500_000, 6_000_000))
        assertThat(progress.saved.value.getValue("vod:501").positionMs).isEqualTo(1_500_000L)
    }

    @Test
    fun `a movie leaves the watch list once it starts playing`() = runTest {
        watchlist.toggle("vod:501", ContentType.VOD)
        val vm = viewModel(PlayerRoute.movie(Fixtures.movie.id, startPositionMs = 0))
        vm.state.test { expectMostRecentItem() }

        vm.onEvent(PlayerEvent.ProgressTick(30_000, 6_000_000))

        assertThat(watchlist.keys.value.getValue(ContentType.VOD)).isEmpty()
    }

    @Test
    fun `a series leaves the watch list once one of its episodes plays`() = runTest {
        watchlist.toggle("series:${Fixtures.series.id}", ContentType.SERIES)
        val vm = viewModel(PlayerRoute.episode(Fixtures.series.id, "90102", 0))
        vm.state.test { expectMostRecentItem() }

        vm.onEvent(PlayerEvent.ProgressTick(30_000, 2_580_000))

        assertThat(watchlist.keys.value.getValue(ContentType.SERIES)).isEmpty()
    }

    @Test
    fun `explicit start position wins over saved progress`() = runTest {
        progress.save(WatchProgress("vod:501", ContentType.VOD, "x", null, 1_200_000, 6_000_000, Fixtures.now, "501"))
        viewModel(PlayerRoute.movie(Fixtures.movie.id, startPositionMs = 42_000)).state.test {
            assertThat(expectMostRecentItem().startPositionMs).isEqualTo(42_000L)
        }
    }

    @Test
    fun `episode resolves through the series details`() = runTest {
        viewModel(PlayerRoute.episode(Fixtures.series.id, "90102", 0)).state.test {
            val state = expectMostRecentItem()
            assertThat(state.source?.url).isEqualTo("http://tv.example.com:8080/series/ayoub/s3cret/90102.mp4")
            assertThat(state.title).isEqualTo("${Fixtures.series.name} S01E02")
        }
    }

    @Test
    fun `unknown item surfaces a not found error`() = runTest {
        viewModel(PlayerRoute.movie(999, 0)).state.test {
            val state = expectMostRecentItem()
            assertThat(state.error?.kind).isEqualTo(PlayerErrorKind.NOT_FOUND)
            assertThat(state.source).isNull()
        }
    }

    @Test
    fun `playback failure from the player is kept until retry`() = runTest {
        val vm = viewModel(PlayerRoute.live(Fixtures.alJazeera.id, null))
        vm.state.test {
            vm.onEvent(PlayerEvent.PlaybackFailed(PlayerError(PlayerErrorKind.SOURCE, "ERROR_CODE_IO_BAD_HTTP_STATUS")))
            assertThat(expectMostRecentItem().error).isNotNull()
            vm.onEvent(PlayerEvent.Retry)
            assertThat(expectMostRecentItem().error).isNull()
        }
    }

    @Test
    fun `picking an audio or subtitle language is remembered as the preference`() = runTest {
        val vm = viewModel(PlayerRoute.movie(Fixtures.movie.id, 0L))
        vm.onEvent(PlayerEvent.RememberLanguage(TrackKind.AUDIO, "ar"))
        vm.onEvent(PlayerEvent.RememberLanguage(TrackKind.SUBTITLE, "fr"))

        assertThat(settings.settings.value.audioLanguage).isEqualTo("ar")
        assertThat(settings.settings.value.subtitleLanguage).isEqualTo("fr")
    }

    @Test
    fun `switching subtitles off clears the subtitle preference`() = runTest {
        settings.settings.value = settings.settings.value.copy(subtitleLanguage = "fr")
        val vm = viewModel(PlayerRoute.movie(Fixtures.movie.id, 0L))
        vm.onEvent(PlayerEvent.RememberLanguage(TrackKind.SUBTITLE, null))

        assertThat(settings.settings.value.subtitleLanguage).isNull()
    }

    @Test
    fun `a new subtitle style is saved and applies to the screen at once`() = runTest {
        val vm = viewModel(PlayerRoute.movie(Fixtures.movie.id, 0L))
        val style = SubtitleStyle(size = SubtitleSize.LARGE, textColor = SubtitleColor.YELLOW)

        vm.onEvent(PlayerEvent.SetSubtitleStyle(style))

        assertThat(settings.settings.value.subtitleStyle).isEqualTo(style)
        assertThat(vm.state.value.settings.subtitleStyle).isEqualTo(style)
    }
}
