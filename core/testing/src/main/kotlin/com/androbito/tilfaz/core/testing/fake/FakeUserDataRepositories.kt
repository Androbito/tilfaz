package com.androbito.tilfaz.core.testing.fake

import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.PlaybackSettingsRepository
import com.androbito.tilfaz.core.domain.repository.RecentChannelsRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.WatchProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeFavoritesRepository : FavoritesRepository {
    val keys = MutableStateFlow<Map<ContentType, Set<String>>>(emptyMap())

    override fun observeKeys(type: ContentType): Flow<Set<String>> = keys.map { it[type].orEmpty() }

    override suspend fun toggle(key: String, type: ContentType) {
        keys.update { map ->
            val current = map[type].orEmpty()
            map + (type to if (key in current) current - key else current + key)
        }
    }
}

class FakeWatchProgressRepository : WatchProgressRepository {
    val saved = MutableStateFlow<Map<String, WatchProgress>>(emptyMap())

    override fun observeContinueWatching(type: ContentType): Flow<List<WatchProgress>> =
        saved.map { map -> map.values.filter { it.contentType == type && !it.isFinished }.sortedByDescending { it.updatedAt } }

    override suspend fun progress(playableKey: String): WatchProgress? = saved.value[playableKey]
    override suspend fun save(progress: WatchProgress) = saved.update { it + (progress.playableKey to progress) }
    override suspend fun clear(playableKey: String) = saved.update { it - playableKey }
}

class FakeRecentChannelsRepository(private val lookup: (Int) -> LiveChannel?) : RecentChannelsRepository {
    val recentIds = MutableStateFlow<List<Int>>(emptyList())

    override fun observeRecent(limit: Int): Flow<List<LiveChannel>> = recentIds.map { ids -> ids.take(limit).mapNotNull(lookup) }
    override suspend fun record(channelId: Int) = recentIds.update { listOf(channelId) + (it - channelId) }
}

class FakePlaybackSettingsRepository(initial: PlaybackSettings = PlaybackSettings()) : PlaybackSettingsRepository {
    val settings = MutableStateFlow(initial)
    override fun observe(): Flow<PlaybackSettings> = settings
    override suspend fun update(transform: (PlaybackSettings) -> PlaybackSettings) = settings.update(transform)
}
