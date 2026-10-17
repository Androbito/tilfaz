package com.androbito.tilfaz.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import com.androbito.tilfaz.core.data.di.IoDispatcher
import com.androbito.tilfaz.core.data.mapper.EpgMapper
import com.androbito.tilfaz.core.data.mapper.toDomain
import com.androbito.tilfaz.core.data.mapper.toEntities
import com.androbito.tilfaz.core.data.mapper.toLiveEntities
import com.androbito.tilfaz.core.data.store.AccountStore
import com.androbito.tilfaz.core.database.CatalogSearch
import com.androbito.tilfaz.core.database.dao.CategoryDao
import com.androbito.tilfaz.core.database.dao.LiveChannelDao
import com.androbito.tilfaz.core.database.dao.SyncStatusDao
import com.androbito.tilfaz.core.database.entity.LiveChannelEntity
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.SyncStatus
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.network.XtreamApi
import com.androbito.tilfaz.core.network.safeApiCall
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class DefaultLiveRepository @Inject constructor(
    private val api: XtreamApi,
    private val categoryDao: CategoryDao,
    private val channelDao: LiveChannelDao,
    private val accountStore: AccountStore,
    syncStatusDao: SyncStatusDao,
    clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) : LiveRepository {

    private val sync = SyncTracker(ContentType.LIVE, syncStatusDao, clock)

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeByType(ContentType.LIVE.name).map { rows -> rows.map { it.toDomain() } }

    override fun pagedItems(categoryId: String?): Flow<PagingData<LiveChannel>> =
        Pager(CatalogPaging.config) { if (categoryId == null) channelDao.pagingAll() else channelDao.pagingByCategory(categoryId) }
            .flow
            .map { page -> page.map(LiveChannelEntity::toDomain) }

    override fun observeCount(): Flow<Int> = channelDao.observeCount()

    override fun observeCategoryCounts(): Flow<Map<String?, Int>> =
        channelDao.observeCategoryCounts().map { rows -> rows.associate { it.categoryId to it.count } }

    override fun search(query: String, limit: Int): Flow<List<LiveChannel>> {
        val normalized = TitleText.normalize(query)
        if (normalized.isEmpty()) return flowOf(emptyList())
        return channelDao.search(CatalogSearch.query("live_channels", normalized, limit)).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeByIds(ids: Set<Int>): Flow<List<LiveChannel>> =
        channelDao.observeByIds(ids.toList()).map { rows -> rows.map { it.toDomain() } }

    override fun observeSyncStatus(): Flow<SyncStatus> = sync.observe()

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        sync.track {
            safeApiCall {
                coroutineScope {
                    val categories = async { api.liveCategories() }
                    val streams = async { api.liveStreams() }
                    categories.await().toEntities(ContentType.LIVE) to streams.await().toLiveEntities()
                }
            }.map { (categories, channels) ->
                categoryDao.replaceAll(ContentType.LIVE.name, categories)
                channelDao.replaceAll(channels)
                sync.markSynced(channels.size)
            }
        }
    }

    override suspend fun firstOf(categoryId: String?): LiveChannel? = withContext(io) { channelDao.first(categoryId)?.toDomain() }

    override suspend fun neighbour(channelId: Int, categoryId: String?, forward: Boolean): LiveChannel? = withContext(io) {
        val next = if (forward) channelDao.after(channelId, categoryId) else channelDao.before(channelId, categoryId)
        // Past either end the list starts over, so a lone channel is its own neighbour.
        (next ?: if (forward) channelDao.first(categoryId) else channelDao.last(categoryId))?.toDomain()
    }

    override suspend fun channel(channelId: Int): LiveChannel? = withContext(io) { channelDao.byId(channelId)?.toDomain() }

    override suspend fun shortEpg(channelId: Int, limit: Int): Result<List<EpgEntry>> = withContext(io) {
        val zone = accountStore.status.first()?.serverTimeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        safeApiCall { api.shortEpg(channelId, limit) }.map { EpgMapper.toDomain(it.listings, channelId, zone) }
    }

    private companion object {
    }
}
