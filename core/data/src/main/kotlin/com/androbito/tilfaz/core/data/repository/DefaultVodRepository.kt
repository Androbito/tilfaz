package com.androbito.tilfaz.core.data.repository

import com.androbito.tilfaz.core.data.di.IoDispatcher
import com.androbito.tilfaz.core.data.mapper.toDomain
import com.androbito.tilfaz.core.data.mapper.toEntities
import com.androbito.tilfaz.core.data.mapper.toMovieEntities
import com.androbito.tilfaz.core.database.dao.CategoryDao
import com.androbito.tilfaz.core.database.dao.MovieDao
import com.androbito.tilfaz.core.database.dao.SyncStatusDao
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.SyncStatus
import com.androbito.tilfaz.core.network.XtreamApi
import com.androbito.tilfaz.core.network.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVodRepository @Inject constructor(
    private val api: XtreamApi,
    private val categoryDao: CategoryDao,
    private val movieDao: MovieDao,
    syncStatusDao: SyncStatusDao,
    clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : VodRepository {

    private val sync = SyncTracker(ContentType.VOD, syncStatusDao, clock)
    private val detailsCache = LruCache<Int, MovieDetails>(DETAILS_CACHE_SIZE)

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeByType(ContentType.VOD.name).map { rows -> rows.map { it.toDomain() } }

    override fun observeItems(categoryId: String?): Flow<List<Movie>> =
        (if (categoryId == null) movieDao.observeAll() else movieDao.observeByCategory(categoryId))
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeSyncStatus(): Flow<SyncStatus> = sync.observe()

    override fun search(query: String): Flow<List<Movie>> =
        movieDao.search(query.trim(), SEARCH_LIMIT).map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        sync.track {
            safeApiCall {
                coroutineScope {
                    val categories = async { api.vodCategories() }
                    val streams = async { api.vodStreams() }
                    categories.await().toEntities(ContentType.VOD) to streams.await().toMovieEntities()
                }
            }.map { (categories, movies) ->
                categoryDao.replaceAll(ContentType.VOD.name, categories)
                movieDao.replaceAll(movies)
                detailsCache.clear()
                sync.markSynced(movies.size)
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
        const val SEARCH_LIMIT = 200
        const val DETAILS_CACHE_SIZE = 50
    }
}
