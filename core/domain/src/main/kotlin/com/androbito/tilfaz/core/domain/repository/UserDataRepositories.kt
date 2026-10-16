package com.androbito.tilfaz.core.domain.repository

import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.WatchProgress
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /** Keys of favourites for one content type, e.g. "live:12", "vod:340", "series:77". */
    fun observeKeys(type: ContentType): Flow<Set<String>>
    suspend fun toggle(key: String, type: ContentType)
}

interface WatchProgressRepository {
    fun observeContinueWatching(type: ContentType): Flow<List<WatchProgress>>
    suspend fun progress(playableKey: String): WatchProgress?
    suspend fun save(progress: WatchProgress)
    suspend fun clear(playableKey: String)
}

interface RecentChannelsRepository {
    fun observeRecent(limit: Int = 20): Flow<List<LiveChannel>>
    suspend fun record(channelId: Int)
}

interface PlaybackSettingsRepository {
    fun observe(): Flow<PlaybackSettings>
    suspend fun update(transform: (PlaybackSettings) -> PlaybackSettings)
}
