package com.androbito.tilfaz.core.testing.fake

import androidx.paging.PagingData
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
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

abstract class FakeCatalogRepository<T : Any>(
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
    override fun observeByIds(ids: Set<Int>): Flow<List<T>> = items.map { list -> list.filter { idOf(it) in ids } }
    override fun pagedItems(categoryId: String?): Flow<PagingData<T>> =
        items.map { list -> PagingData.from(if (categoryId == null) list else list.filter { categoryOf(it) == categoryId }) }
    override fun observeCount(): Flow<Int> = items.map { it.size }
    override fun observeCategoryCounts(): Flow<Map<String?, Int>> = items.map { list -> list.groupingBy(categoryOf).eachCount() }

    /** Keeps the catalog order: how results are ranked is the database's job and is tested there. */
    override fun search(query: String, limit: Int): Flow<List<T>> {
        val tokens = TitleText.normalize(query).split(' ').filter { it.isNotEmpty() }
        return items.map { list ->
            if (tokens.isEmpty()) emptyList() else list.filter { item -> TitleText.searchKey(nameOf(item)).let { key -> tokens.all { it in key } } }.take(limit)
        }
    }
    override fun observeSyncStatus(): Flow<SyncStatus> = syncStatus

    override suspend fun refresh(): Result<Unit> {
        refreshCalls++
        // Like the real repositories: a successful refresh records when it happened, a failed one records nothing.
        refreshResult.onSuccess { syncStatus.update { it.copy(itemCount = items.value.size, lastSyncedAt = Fixtures.fixedClock.instant()) } }
        return refreshResult
    }
}

class FakeLiveRepository : FakeCatalogRepository<LiveChannel>(ContentType.LIVE, { it.id }, { it.categoryId }, { it.name }), LiveRepository {
    val epg = mutableMapOf<Int, Result<List<EpgEntry>>>()
    val epgRequests = mutableListOf<Int>()

    override suspend fun channel(channelId: Int): LiveChannel? = items.value.firstOrNull { it.id == channelId }

    override suspend fun firstOf(categoryId: String?): LiveChannel? =
        items.value.firstOrNull { categoryId == null || it.categoryId == categoryId }

    override suspend fun neighbour(channelId: Int, categoryId: String?, forward: Boolean): LiveChannel? {
        val list = items.value.filter { categoryId == null || it.categoryId == categoryId }
        val index = list.indexOfFirst { it.id == channelId }
        if (index < 0) return null
        return list[(index + if (forward) 1 else -1 + list.size) % list.size]
    }

    override suspend fun shortEpg(channelId: Int, limit: Int): Result<List<EpgEntry>> {
        epgRequests += channelId
        return epg[channelId] ?: Result.success(emptyList())
    }
}

class FakeVodRepository : FakeCatalogRepository<Movie>(ContentType.VOD, { it.id }, { it.categoryId }, { it.name }), VodRepository {
    var detailsResult: ((Int) -> Result<MovieDetails>)? = null

    override fun observeRecent(limit: Int): Flow<List<Movie>> =
        items.map { list -> list.sortedByDescending { it.addedAt }.take(limit) }
    override fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<Movie>> =
        items.map { list -> list.filter { it.categoryId == categoryId && it.id != excludeId }.take(limit) }

    override suspend fun movie(movieId: Int): Movie? = items.value.firstOrNull { it.id == movieId }
    override suspend fun details(movieId: Int): Result<MovieDetails> =
        detailsResult?.invoke(movieId) ?: Result.failure(IllegalStateException("no details configured"))
}

class FakeSeriesRepository : FakeCatalogRepository<Series>(ContentType.SERIES, { it.id }, { it.categoryId }, { it.name }), SeriesRepository {
    var detailsResult: ((Int) -> Result<SeriesDetails>)? = null

    override fun observeRecent(limit: Int): Flow<List<Series>> =
        items.map { list -> list.sortedByDescending { it.lastModifiedAt }.take(limit) }
    override fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<Series>> =
        items.map { list -> list.filter { it.categoryId == categoryId && it.id != excludeId }.take(limit) }

    override suspend fun series(seriesId: Int): Series? = items.value.firstOrNull { it.id == seriesId }
    override suspend fun details(seriesId: Int): Result<SeriesDetails> =
        detailsResult?.invoke(seriesId) ?: Result.failure(IllegalStateException("no details configured"))
}
