package com.androbito.tilfaz.core.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.androbito.tilfaz.core.model.BufferProfile
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import com.androbito.tilfaz.core.model.SubtitleStyle
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStore

@Singleton
class PlaybackSettingsStore @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<PlaybackSettings> = dataStore.data.map { prefs ->
        PlaybackSettings(
            liveFormat = StreamFormat.fromExtension(prefs[Keys.LIVE_FORMAT]) ?: StreamFormat.MPEG_TS,
            userAgent = prefs[Keys.USER_AGENT]?.takeIf { it.isNotBlank() } ?: PlaybackSettings.DEFAULT_USER_AGENT,
            bufferProfile = prefs[Keys.BUFFER]?.let { name -> BufferProfile.entries.firstOrNull { it.name == name } } ?: BufferProfile.BALANCED,
            resumePlayback = prefs[Keys.RESUME] ?: true,
            audioLanguage = prefs[Keys.AUDIO_LANGUAGE]?.takeIf { it.isNotBlank() },
            subtitleLanguage = prefs[Keys.SUBTITLE_LANGUAGE]?.takeIf { it.isNotBlank() },
            subtitleStyle = SubtitleStyle(
                size = enumOf(prefs[Keys.SUBTITLE_SIZE], SubtitleSize.MEDIUM),
                textColor = enumOf(prefs[Keys.SUBTITLE_COLOR], SubtitleColor.WHITE),
                background = enumOf(prefs[Keys.SUBTITLE_BACKGROUND], SubtitleBackground.TRANSLUCENT)
            )
        )
    }.distinctUntilChanged()

    suspend fun update(transform: (PlaybackSettings) -> PlaybackSettings) {
        val next = transform(settings.first())
        dataStore.edit { prefs ->
            prefs[Keys.LIVE_FORMAT] = next.liveFormat.extension
            prefs[Keys.USER_AGENT] = next.userAgent
            prefs[Keys.BUFFER] = next.bufferProfile.name
            prefs[Keys.RESUME] = next.resumePlayback
            prefs.putOrRemove(Keys.AUDIO_LANGUAGE, next.audioLanguage)
            prefs.putOrRemove(Keys.SUBTITLE_LANGUAGE, next.subtitleLanguage)
            prefs[Keys.SUBTITLE_SIZE] = next.subtitleStyle.size.name
            prefs[Keys.SUBTITLE_COLOR] = next.subtitleStyle.textColor.name
            prefs[Keys.SUBTITLE_BACKGROUND] = next.subtitleStyle.background.name
        }
    }

    /** A name stored by another version of the app that this one does not know falls back to the default. */
    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E = enumValues<E>().firstOrNull { it.name == name } ?: default

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private object Keys {
        val LIVE_FORMAT = stringPreferencesKey("live_format")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val BUFFER = stringPreferencesKey("buffer_profile")
        val RESUME = booleanPreferencesKey("resume_playback")
        val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val SUBTITLE_SIZE = stringPreferencesKey("subtitle_size")
        val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        val SUBTITLE_BACKGROUND = stringPreferencesKey("subtitle_background")
    }
}
