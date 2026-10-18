package com.androbito.tilfaz.core.data.store

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlaybackSettingsStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private fun TestScope.newDataStore() = PreferenceDataStoreFactory.create(scope = this, produceFile = { File(folder.root, "settings.preferences_pb") })

    @Test
    fun `a fresh install has no language preference and a readable subtitle style`() = runTest(UnconfinedTestDispatcher()) {
        val settings = PlaybackSettingsStore(newDataStore()).settings.first()

        assertThat(settings.audioLanguage).isNull()
        assertThat(settings.subtitleLanguage).isNull()
        assertThat(settings.subtitleStyle.size).isEqualTo(SubtitleSize.MEDIUM)
        assertThat(settings.subtitleStyle.textColor).isEqualTo(SubtitleColor.WHITE)
        assertThat(settings.subtitleStyle.background).isEqualTo(SubtitleBackground.TRANSLUCENT)
    }

    @Test
    fun `languages and subtitle style survive a restart`() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = newDataStore()
        PlaybackSettingsStore(dataStore).update {
            it.copy(
                audioLanguage = "ar",
                subtitleLanguage = "en",
                subtitleStyle = it.subtitleStyle.copy(size = SubtitleSize.LARGE, textColor = SubtitleColor.YELLOW, background = SubtitleBackground.BLACK)
            )
        }

        val reloaded = PlaybackSettingsStore(dataStore).settings.first()
        assertThat(reloaded.audioLanguage).isEqualTo("ar")
        assertThat(reloaded.subtitleLanguage).isEqualTo("en")
        assertThat(reloaded.subtitleStyle.size).isEqualTo(SubtitleSize.LARGE)
        assertThat(reloaded.subtitleStyle.textColor).isEqualTo(SubtitleColor.YELLOW)
        assertThat(reloaded.subtitleStyle.background).isEqualTo(SubtitleBackground.BLACK)
    }

    @Test
    fun `clearing a language goes back to no preference`() = runTest(UnconfinedTestDispatcher()) {
        val store = PlaybackSettingsStore(newDataStore())
        store.update { it.copy(subtitleLanguage = "fr") }

        store.update { it.copy(subtitleLanguage = null) }

        assertThat(store.settings.first().subtitleLanguage).isNull()
    }

    @Test
    fun `a stored value this version does not know falls back to the default`() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = newDataStore()
        dataStore.edit { it[stringPreferencesKey("subtitle_size")] = "GIGANTIC_FROM_THE_FUTURE" }

        assertThat(PlaybackSettingsStore(dataStore).settings.first().subtitleStyle.size).isEqualTo(SubtitleSize.MEDIUM)
        assertThat(PlaybackSettings().subtitleStyle.size).isEqualTo(SubtitleSize.MEDIUM)
    }

    @Test
    fun `clearing puts every setting back to what a fresh install has`() = runTest(UnconfinedTestDispatcher()) {
        val store = PlaybackSettingsStore(newDataStore())
        store.update {
            it.copy(
                audioLanguage = "ar",
                subtitleLanguage = "fr",
                userAgent = "Custom/1.0",
                resumePlayback = false,
                subtitleStyle = it.subtitleStyle.copy(size = SubtitleSize.LARGE, textColor = SubtitleColor.YELLOW)
            )
        }

        store.clear()

        assertThat(store.settings.first()).isEqualTo(PlaybackSettings())
    }
}
