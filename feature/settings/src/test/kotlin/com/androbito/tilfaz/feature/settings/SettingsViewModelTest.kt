package com.androbito.tilfaz.feature.settings

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.domain.usecase.SyncCatalogUseCase
import com.androbito.tilfaz.core.model.BufferProfile
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import com.androbito.tilfaz.core.model.SubtitleStyle
import com.androbito.tilfaz.core.testing.fake.FakeAccountRepository
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakePlaybackSettingsRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val settings = FakePlaybackSettingsRepository()
    private val account = FakeAccountRepository(Fixtures.account).apply { status.value = Fixtures.activeStatus }
    private val live = FakeLiveRepository()
    private val vod = FakeVodRepository()
    private val series = FakeSeriesRepository()
    private val viewModel by lazy { SettingsViewModel(settings, account, SyncCatalogUseCase(live, vod, series)) }

    @Test
    fun `playback choices are persisted`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(SettingsEvent.SetLiveFormat(StreamFormat.HLS))
            viewModel.onEvent(SettingsEvent.SetBuffer(BufferProfile.STABLE))
            viewModel.onEvent(SettingsEvent.SetResume(false))
            val s = expectMostRecentItem().settings
            assertThat(s.liveFormat).isEqualTo(StreamFormat.HLS)
            assertThat(s.bufferProfile).isEqualTo(BufferProfile.STABLE)
            assertThat(s.resumePlayback).isFalse()
        }
    }

    @Test
    fun `preferred languages are persisted and can be cleared`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(SettingsEvent.SetAudioLanguage("ar"))
            viewModel.onEvent(SettingsEvent.SetSubtitleLanguage("en"))
            val chosen = expectMostRecentItem().settings
            assertThat(chosen.audioLanguage).isEqualTo("ar")
            assertThat(chosen.subtitleLanguage).isEqualTo("en")

            viewModel.onEvent(SettingsEvent.SetSubtitleLanguage(null))
            assertThat(expectMostRecentItem().settings.subtitleLanguage).isNull()
        }
    }

    @Test
    fun `each subtitle style choice changes only itself`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(SettingsEvent.SetSubtitleSize(SubtitleSize.EXTRA_LARGE))
            viewModel.onEvent(SettingsEvent.SetSubtitleColor(SubtitleColor.CYAN))
            viewModel.onEvent(SettingsEvent.SetSubtitleBackground(SubtitleBackground.NONE))

            assertThat(expectMostRecentItem().settings.subtitleStyle).isEqualTo(
                SubtitleStyle(SubtitleSize.EXTRA_LARGE, SubtitleColor.CYAN, SubtitleBackground.NONE)
            )
        }
    }

    @Test
    fun `refreshing the catalogs downloads all three again and then says it is done`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(SettingsEvent.RefreshCatalogs)

            assertThat(live.refreshCalls).isEqualTo(1)
            assertThat(vod.refreshCalls).isEqualTo(1)
            assertThat(series.refreshCalls).isEqualTo(1)
            assertThat(expectMostRecentItem().isRefreshingCatalogs).isFalse()
        }
    }

    @Test
    fun `blank user agent falls back to the default`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(SettingsEvent.SetUserAgent("  VLC/3.0.20  "))
            assertThat(expectMostRecentItem().settings.userAgent).isEqualTo("VLC/3.0.20")
            viewModel.onEvent(SettingsEvent.SetUserAgent("   "))
            assertThat(expectMostRecentItem().settings.userAgent).isEqualTo(PlaybackSettings.DEFAULT_USER_AGENT)
        }
    }

    @Test
    fun `sign out needs confirmation and then clears the account`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(SettingsEvent.SignOutRequested)
            assertThat(expectMostRecentItem().confirmSignOut).isTrue()
            viewModel.onEvent(SettingsEvent.SignOutCancelled)
            assertThat(expectMostRecentItem().confirmSignOut).isFalse()
            assertThat(account.signedOut).isFalse()

            viewModel.onEvent(SettingsEvent.SignOutRequested)
            viewModel.onEvent(SettingsEvent.SignOutConfirmed)
            val state = expectMostRecentItem()
            assertThat(state.signedOut).isTrue()
            assertThat(account.signedOut).isTrue()
        }
    }
}
