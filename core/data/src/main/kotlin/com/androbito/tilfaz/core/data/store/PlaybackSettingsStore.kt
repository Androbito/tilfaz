package com.androbito.tilfaz.core.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.androbito.tilfaz.core.model.BufferProfile
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStore

@Singleton
class PlaybackSettingsStore @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<PlaybackSettings> = dataStore.data.map { prefs ->
        PlaybackSettings(
            liveFormat = StreamFormat.fromExtension(prefs[Keys.LIVE_FORMAT]) ?: StreamFormat.MPEG_TS,
            userAgent = prefs[Keys.USER_AGENT]?.takeIf { it.isNotBlank() } ?: PlaybackSettings.DEFAULT_USER_AGENT,
            bufferProfile = prefs[Keys.BUFFER]?.let { name -> BufferProfile.entries.firstOrNull { it.name == name } } ?: BufferProfile.BALANCED,
            resumePlayback = prefs[Keys.RESUME] ?: true,
        )
    }.distinctUntilChanged()

    suspend fun update(transform: (PlaybackSettings) -> PlaybackSettings) {
        val next = transform(settings.first())
        dataStore.edit { prefs ->
            prefs[Keys.LIVE_FORMAT] = next.liveFormat.extension
            prefs[Keys.USER_AGENT] = next.userAgent
            prefs[Keys.BUFFER] = next.bufferProfile.name
            prefs[Keys.RESUME] = next.resumePlayback
        }
    }

    private object Keys {
        val LIVE_FORMAT = stringPreferencesKey("live_format")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val BUFFER = stringPreferencesKey("buffer_profile")
        val RESUME = booleanPreferencesKey("resume_playback")
    }
}
