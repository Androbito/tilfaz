package com.androbito.tilfaz.core.testing.fake

import com.androbito.tilfaz.core.domain.repository.CatalogRepository
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

abstract class FakeCatalogRepository<T>(
    private val type: ContentType,
    private val idOf: (T) -> Int,
    private val categoryOf: (T) -> String?,
    private val nameOf: (T) -> String
) : CatalogRepository<T> {
    val categories = MutableStateFlow<List<Category>>(emptyList())
    val items = MutableStateFlow<List<T>>(emptyList())
    val syncStatus = MutableStateFlow(SyncStatus(type, lastSyncedAt = null, itemCount = 0))
    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshCalls = 0

    override fun observeCategories(): Flow<List<Category>> = categories
    override fun observeItems(categoryId: String?): Flow<List<T>> =
        items.map { list -> if (categoryId == null) list else list.filter { categoryOf(it) == categoryId } }
    override fun observeByIds(ids: Set<Int>): Flow<List<T>> = items.map { list -> list.filter { idOf(it) in ids } }
    override fun observeSyncStatus(): Flow<SyncStatus> = syncStatus
    override fun search(query: String): Flow<List<T>> =
        items.map { list -> list.filter { nameOf(it).contains(query, ignoreCase = true) } }

    override suspend fun refresh(): Result<Unit> {
        refreshCalls++
        refreshResult.onSuccess { syncStatus.update { it.copy(itemCount = items.value.size) } }
        return refreshResult
    }
}

class FakeLiveRepository : FakeCatalogRepository<LiveChannel>(ContentType.LIVE, { it.id }, { it.categoryId }, { it.name }), LiveRepository {
    val epg = mutableMapOf<Int, Result<List<EpgEntry>>>()
    val epgRequests = mutableListOf<Int>()

    override suspend fun channel(channelId: Int): LiveChannel? = items.value.firstOrNull { it.id == channelId }

    override suspend fun shortEpg(channelId: Int, limit: Int): Result<List<EpgEntry>> {
        epgRequests += channelId
        return epg[channelId] ?: Result.success(emptyList())
    }
}

class FakeVodRepository : FakeCatalogRepository<Movie>(ContentType.VOD, { it.id }, { it.categoryId }, { it.name }), VodRepository {
    var detailsResult: ((Int) -> Result<MovieDetails>)? = null

    override suspend fun movie(movieId: Int): Movie? = items.value.firstOrNull { it.id == movieId }
    override suspend fun details(movieId: Int): Result<MovieDetails> =
        detailsResult?.invoke(movieId) ?: Result.failure(IllegalStateException("no details configured"))
}

class FakeSeriesRepository : FakeCatalogRepository<Series>(ContentType.SERIES, { it.id }, { it.categoryId }, { it.name }), SeriesRepository {
    var detailsResult: ((Int) -> Result<SeriesDetails>)? = null

    override suspend fun series(seriesId: Int): Series? = items.value.firstOrNull { it.id == seriesId }
    override suspend fun details(seriesId: Int): Result<SeriesDetails> =
        detailsResult?.invoke(seriesId) ?: Result.failure(IllegalStateException("no details configured"))
}
