package com.androbito.tilfaz.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import com.androbito.tilfaz.core.data.di.IoDispatcher
import com.androbito.tilfaz.core.data.mapper.toDomain
import com.androbito.tilfaz.core.data.mapper.toEntities
import com.androbito.tilfaz.core.data.mapper.toMovieEntities
import com.androbito.tilfaz.core.database.CatalogSearch
import com.androbito.tilfaz.core.database.dao.CategoryDao
import com.androbito.tilfaz.core.database.dao.MovieDao
import com.androbito.tilfaz.core.database.dao.SyncStatusDao
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.SyncStatus
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.network.XtreamApi
import com.androbito.tilfaz.core.network.dto.VodStreamDto
import com.androbito.tilfaz.core.network.safeApiCall
import com.androbito.tilfaz.core.network.streamArray
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class DefaultVodRepository @Inject constructor(
    private val api: XtreamApi,
    private val categoryDao: CategoryDao,
    private val movieDao: MovieDao,
    private val json: Json,
    syncStatusDao: SyncStatusDao,
    clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) : VodRepository {

    private val sync = SyncTracker(ContentType.VOD, syncStatusDao, clock)
    private val detailsCache = LruCache<Int, MovieDetails>(DETAILS_CACHE_SIZE)

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeByType(ContentType.VOD.name).map { rows -> rows.map { it.toDomain() } }

    override fun pagedItems(categoryId: String?): Flow<PagingData<Movie>> =
        Pager(CatalogPaging.config) { if (categoryId == null) movieDao.pagingAll() else movieDao.pagingByCategory(categoryId) }
            .flow
            .map { page -> page.map(MovieEntity::toDomain) }

    override fun observeCount(): Flow<Int> = movieDao.observeCount()

    override fun observeCategoryCounts(): Flow<Map<String?, Int>> =
        movieDao.observeCategoryCounts().map { rows -> rows.associate { it.categoryId to it.count } }

    override fun search(query: String, limit: Int): Flow<List<Movie>> {
        val normalized = TitleText.normalize(query)
        if (normalized.isEmpty()) return flowOf(emptyList())
        return movieDao.search(CatalogSearch.query("movies", normalized, limit)).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeRecent(limit: Int): Flow<List<Movie>> = movieDao.observeRecentlyAdded(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<Movie>> =
        movieDao.observeRelated(categoryId, excludeId, limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeByIds(ids: Set<Int>): Flow<List<Movie>> =
        movieDao.observeByIds(ids.toList()).map { rows -> rows.map { it.toDomain() } }

    override fun observeSyncStatus(): Flow<SyncStatus> = sync.observe()

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        sync.track {
            safeApiCall {
                val categories = api.vodCategories().toEntities(ContentType.VOD)
                // Read as a stream and saved a chunk at a time: the list is far larger than a TV has memory for.
                val stored = api.vodStreams().use { body ->
                    movieDao.replaceAll(
                        json.streamArray(body, VodStreamDto.serializer())
                            .chunked(CatalogSync.CHUNK_SIZE)
                            .mapIndexed { index, chunk -> chunk.toMovieEntities(startIndex = index * CatalogSync.CHUNK_SIZE) }
                    )
                }
                categoryDao.replaceAll(ContentType.VOD.name, categories)
                detailsCache.clear()
                sync.markSynced(stored)
            }
        }
    }

    override suspend fun movie(movieId: Int): Movie? = withContext(io) { movieDao.byId(movieId)?.toDomain() }

    override suspend fun details(movieId: Int): Result<MovieDetails> = withContext(io) {
        detailsCache[movieId]?.let { return@withContext Result.success(it) }
        val fallback = movieDao.byId(movieId)?.toDomain()
            ?: return@withContext Result.failure(DomainException(DomainError.BadResponse("unknown movie $movieId")))
        safeApiCall { api.vodInfo(movieId) }
            .map { it.toDomain(fallback) }
            .onSuccess { detailsCache[movieId] = it }
    }

    private companion object {
        const val DETAILS_CACHE_SIZE = 50
    }
}
