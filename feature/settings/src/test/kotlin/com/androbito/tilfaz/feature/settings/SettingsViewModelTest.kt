package com.androbito.tilfaz.feature.settings

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.model.BufferProfile
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.testing.fake.FakeAccountRepository
import com.androbito.tilfaz.core.testing.fake.FakePlaybackSettingsRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val settings = FakePlaybackSettingsRepository()
    private val account = FakeAccountRepository(Fixtures.account).apply { status.value = Fixtures.activeStatus }
    private val viewModel by lazy { SettingsViewModel(settings, account) }

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
