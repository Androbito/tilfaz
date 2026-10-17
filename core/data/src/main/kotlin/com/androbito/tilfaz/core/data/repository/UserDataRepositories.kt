package com.androbito.tilfaz.core.data.repository

import com.androbito.tilfaz.core.data.di.IoDispatcher
import com.androbito.tilfaz.core.data.mapper.toDomain
import com.androbito.tilfaz.core.data.store.PlaybackSettingsStore
import com.androbito.tilfaz.core.database.dao.FavoriteDao
import com.androbito.tilfaz.core.database.dao.LiveChannelDao
import com.androbito.tilfaz.core.database.dao.RecentChannelDao
import com.androbito.tilfaz.core.database.dao.WatchProgressDao
import com.androbito.tilfaz.core.database.entity.FavoriteEntity
import com.androbito.tilfaz.core.database.entity.RecentChannelEntity
import com.androbito.tilfaz.core.database.entity.WatchProgressEntity
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.PlaybackSettingsRepository
import com.androbito.tilfaz.core.domain.repository.RecentChannelsRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.WatchProgress
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RoomFavoritesRepository @Inject constructor(
    private val dao: FavoriteDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) : FavoritesRepository {
    override fun observeKeys(type: ContentType): Flow<Set<String>> = dao.observeKeys(type.name).map { it.toSet() }

    override suspend fun toggle(key: String, type: ContentType) = withContext(io) {
        if (dao.exists(key)) dao.delete(key) else dao.insert(FavoriteEntity(key, type.name, clock.millis()))
    }
}

@Singleton
class RoomWatchProgressRepository @Inject constructor(
    private val dao: WatchProgressDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : WatchProgressRepository {
    override fun observeContinueWatching(type: ContentType): Flow<List<WatchProgress>> =
        dao.observeUnfinishedByType(type.name, WatchProgress.FINISHED_THRESHOLD.toDouble(), CONTINUE_WATCHING_LIMIT)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeByKeyPrefix(prefix: String): Flow<List<WatchProgress>> =
        dao.observeByKeyPrefix(prefix).map { rows -> rows.map { it.toDomain() } }

    override suspend fun progress(playableKey: String): WatchProgress? = withContext(io) { dao.byKey(playableKey)?.toDomain() }

    override suspend fun save(progress: WatchProgress) = withContext(io) { dao.upsert(progress.toEntity()) }

    override suspend fun clear(playableKey: String) = withContext(io) { dao.delete(playableKey) }

    private fun WatchProgressEntity.toDomain() = WatchProgress(
        playableKey = playableKey,
        contentType = ContentType.valueOf(type),
        title = title,
        posterUrl = posterUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = Instant.ofEpochMilli(updatedAtMillis),
        reference = reference
    )

    private fun WatchProgress.toEntity() = WatchProgressEntity(
        playableKey = playableKey,
        type = contentType.name,
        title = title,
        posterUrl = posterUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAtMillis = updatedAt.toEpochMilli(),
        reference = reference
    )

    private companion object {
        const val CONTINUE_WATCHING_LIMIT = 30
    }
}

@Singleton
class RoomRecentChannelsRepository @Inject constructor(
    private val recentDao: RecentChannelDao,
    private val channelDao: LiveChannelDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) : RecentChannelsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRecent(limit: Int): Flow<List<LiveChannel>> = recentDao.observeRecent(limit).flatMapLatest { recent ->
        if (recent.isEmpty()) {
            flowOf(emptyList())
        } else {
            val order = recent.withIndex().associate { it.value.channelId to it.index }
            channelDao.observeByIds(recent.map { it.channelId }).map { rows ->
                rows.sortedBy { order[it.id] ?: Int.MAX_VALUE }.map { it.toDomain() }
            }
        }
    }

    override suspend fun record(channelId: Int) = withContext(io) {
        recentDao.upsert(RecentChannelEntity(channelId, clock.millis()))
        recentDao.trim(HISTORY_SIZE)
    }

    private companion object {
        const val HISTORY_SIZE = 30
    }
}

@Singleton
class DataStorePlaybackSettingsRepository @Inject constructor(
    private val store: PlaybackSettingsStore
) : PlaybackSettingsRepository {
    override fun observe(): Flow<PlaybackSettings> = store.settings
    override suspend fun update(transform: (PlaybackSettings) -> PlaybackSettings) = store.update(transform)
}
