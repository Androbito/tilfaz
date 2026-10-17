package com.androbito.tilfaz.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import com.androbito.tilfaz.core.data.di.IoDispatcher
import com.androbito.tilfaz.core.data.mapper.toDomain
import com.androbito.tilfaz.core.data.mapper.toEntities
import com.androbito.tilfaz.core.data.mapper.toSeriesEntities
import com.androbito.tilfaz.core.database.CatalogSearch
import com.androbito.tilfaz.core.database.dao.CategoryDao
import com.androbito.tilfaz.core.database.dao.SeriesDao
import com.androbito.tilfaz.core.database.dao.SyncStatusDao
import com.androbito.tilfaz.core.database.entity.SeriesEntity
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.SyncStatus
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.network.XtreamApi
import com.androbito.tilfaz.core.network.safeApiCall
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class DefaultSeriesRepository @Inject constructor(
    private val api: XtreamApi,
    private val categoryDao: CategoryDao,
    private val seriesDao: SeriesDao,
    private val json: Json,
    syncStatusDao: SyncStatusDao,
    clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) : SeriesRepository {

    private val sync = SyncTracker(ContentType.SERIES, syncStatusDao, clock)
    private val detailsCache = LruCache<Int, SeriesDetails>(DETAILS_CACHE_SIZE)

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeByType(ContentType.SERIES.name).map { rows -> rows.map { it.toDomain() } }

    override fun observeItems(categoryId: String?): Flow<List<Series>> =
        (if (categoryId == null) seriesDao.observeAll() else seriesDao.observeByCategory(categoryId))
            .map { rows -> rows.map { it.toDomain() } }

    override fun pagedItems(categoryId: String?): Flow<PagingData<Series>> =
        Pager(CatalogPaging.config) { if (categoryId == null) seriesDao.pagingAll() else seriesDao.pagingByCategory(categoryId) }
            .flow
            .map { page -> page.map(SeriesEntity::toDomain) }

    override fun observeCount(): Flow<Int> = seriesDao.observeCount()

    override fun observeCategoryCounts(): Flow<Map<String?, Int>> =
        seriesDao.observeCategoryCounts().map { rows -> rows.associate { it.categoryId to it.count } }

    override fun search(query: String, limit: Int): Flow<List<Series>> {
        val normalized = TitleText.normalize(query)
        if (normalized.isEmpty()) return flowOf(emptyList())
        return seriesDao.search(CatalogSearch.query("series", normalized, limit)).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeRecent(limit: Int): Flow<List<Series>> = seriesDao.observeRecentlyAdded(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<Series>> =
        seriesDao.observeRelated(categoryId, excludeId, limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeByIds(ids: Set<Int>): Flow<List<Series>> =
        seriesDao.observeByIds(ids.toList()).map { rows -> rows.map { it.toDomain() } }

    override fun observeSyncStatus(): Flow<SyncStatus> = sync.observe()

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        sync.track {
            safeApiCall {
                coroutineScope {
                    val categories = async { api.seriesCategories() }
                    val series = async { api.series() }
                    categories.await().toEntities(ContentType.SERIES) to series.await().toSeriesEntities()
                }
            }.map { (categories, series) ->
                categoryDao.replaceAll(ContentType.SERIES.name, categories)
                seriesDao.replaceAll(series)
                detailsCache.clear()
                sync.markSynced(series.size)
            }
        }
    }

    override suspend fun series(seriesId: Int): Series? = withContext(io) { seriesDao.byId(seriesId)?.toDomain() }

    override suspend fun details(seriesId: Int): Result<SeriesDetails> = withContext(io) {
        detailsCache[seriesId]?.let { return@withContext Result.success(it) }
        val fallback = seriesDao.byId(seriesId)?.toDomain()
            ?: return@withContext Result.failure(DomainException(DomainError.BadResponse("unknown series $seriesId")))
        safeApiCall { api.seriesInfo(seriesId) }
            .map { it.toDomain(fallback, json) }
            .onSuccess { detailsCache[seriesId] = it }
    }

    private companion object {
        const val DETAILS_CACHE_SIZE = 50
    }
}
