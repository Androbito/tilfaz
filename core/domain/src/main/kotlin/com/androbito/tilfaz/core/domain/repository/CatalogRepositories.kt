package com.androbito.tilfaz.core.domain.repository

import androidx.paging.PagingData
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/** Common shape of the three catalogs: a locally cached list that a refresh syncs from the server. */
interface CatalogRepository<T : Any> {
    fun observeCategories(): Flow<List<Category>>
    fun observeItems(categoryId: String?): Flow<List<T>>

    /**
     * The catalog (or one category of it) as pages, in the provider's order. Only a window of it is held in memory,
     * which is what lets a hundred thousand titles be browsed on a TV with little free RAM.
     */
    fun pagedItems(categoryId: String?): Flow<PagingData<T>>

    fun observeCount(): Flow<Int>

    /** How many items each provider category holds (key null: items without a category). */
    fun observeCategoryCounts(): Flow<Map<String?, Int>>

    /** The best [limit] matches for what the user typed, best first; nothing for a blank query. */
    fun search(query: String, limit: Int): Flow<List<T>>

    /** The items with these ids, in no particular order. Loads only those rows, not the catalog. */
    fun observeByIds(ids: Set<Int>): Flow<List<T>>

    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun refresh(): Result<Unit>
}

interface LiveRepository : CatalogRepository<LiveChannel> {
    suspend fun channel(channelId: Int): LiveChannel?
    suspend fun shortEpg(channelId: Int, limit: Int = 4): Result<List<EpgEntry>>
}

interface VodRepository : CatalogRepository<Movie> {
    /** The [limit] most recently added movies, newest first. */
    fun observeRecent(limit: Int): Flow<List<Movie>>

    /** A few other movies of the same category, in the provider's order. */
    fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<Movie>>

    suspend fun movie(movieId: Int): Movie?
    suspend fun details(movieId: Int): Result<MovieDetails>
}

interface SeriesRepository : CatalogRepository<Series> {
    /** The [limit] most recently updated series, newest first. */
    fun observeRecent(limit: Int): Flow<List<Series>>

    /** A few other series of the same category, in the provider's order. */
    fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<Series>>

    suspend fun series(seriesId: Int): Series?
    suspend fun details(seriesId: Int): Result<SeriesDetails>
}
