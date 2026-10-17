package com.androbito.tilfaz.core.domain.repository

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
interface CatalogRepository<T> {
    fun observeCategories(): Flow<List<Category>>
    fun observeItems(categoryId: String?): Flow<List<T>>

    /** The items with these ids, in no particular order. Loads only those rows, not the catalog. */
    fun observeByIds(ids: Set<Int>): Flow<List<T>>

    fun observeSyncStatus(): Flow<SyncStatus>
    fun search(query: String): Flow<List<T>>
    suspend fun refresh(): Result<Unit>
}

interface LiveRepository : CatalogRepository<LiveChannel> {
    suspend fun channel(channelId: Int): LiveChannel?
    suspend fun shortEpg(channelId: Int, limit: Int = 4): Result<List<EpgEntry>>
}

interface VodRepository : CatalogRepository<Movie> {
    suspend fun movie(movieId: Int): Movie?
    suspend fun details(movieId: Int): Result<MovieDetails>
}

interface SeriesRepository : CatalogRepository<Series> {
    suspend fun series(seriesId: Int): Series?
    suspend fun details(seriesId: Int): Result<SeriesDetails>
}
