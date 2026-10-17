package com.androbito.tilfaz.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.androbito.tilfaz.core.database.entity.CategoryEntity
import com.androbito.tilfaz.core.database.entity.LiveChannelEntity
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.database.entity.SeriesEntity
import com.androbito.tilfaz.core.database.entity.SyncStatusEntity
import kotlinx.coroutines.flow.Flow

/** How many rows of a catalog table fall in each provider category (null: rows without one). */
data class CategoryCount(val categoryId: String?, val count: Int)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY position")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Transaction
    suspend fun replaceAll(type: String, categories: List<CategoryEntity>) {
        deleteByType(type)
        insertAll(categories)
    }
}

@Dao
interface LiveChannelDao {
    @Query("SELECT * FROM live_channels ORDER BY position")
    fun pagingAll(): PagingSource<Int, LiveChannelEntity>

    @Query("SELECT * FROM live_channels WHERE categoryId = :categoryId ORDER BY position")
    fun pagingByCategory(categoryId: String): PagingSource<Int, LiveChannelEntity>

    @Query("SELECT COUNT(*) FROM live_channels")
    fun observeCount(): Flow<Int>

    @Query("SELECT categoryId, COUNT(*) AS count FROM live_channels GROUP BY categoryId")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    /** Build the query with [com.androbito.tilfaz.core.database.CatalogSearch.query]. */
    @RawQuery(observedEntities = [LiveChannelEntity::class])
    fun search(query: SupportSQLiteQuery): Flow<List<LiveChannelEntity>>

    @Query("SELECT * FROM live_channels WHERE id IN (:ids)")
    fun observeByIds(ids: List<Int>): Flow<List<LiveChannelEntity>>

    @Query("SELECT * FROM live_channels WHERE id = :id")
    suspend fun byId(id: Int): LiveChannelEntity?

    @Query("SELECT * FROM live_channels WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY position LIMIT 1")
    suspend fun first(categoryId: String?): LiveChannelEntity?

    @Query("SELECT * FROM live_channels WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY position DESC LIMIT 1")
    suspend fun last(categoryId: String?): LiveChannelEntity?

    @Query(
        "SELECT * FROM live_channels WHERE (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND position > (SELECT position FROM live_channels WHERE id = :id) ORDER BY position LIMIT 1"
    )
    suspend fun after(id: Int, categoryId: String?): LiveChannelEntity?

    @Query(
        "SELECT * FROM live_channels WHERE (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND position < (SELECT position FROM live_channels WHERE id = :id) ORDER BY position DESC LIMIT 1"
    )
    suspend fun before(id: Int, categoryId: String?): LiveChannelEntity?

    @Query("SELECT COUNT(*) FROM live_channels")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<LiveChannelEntity>)

    @Query("DELETE FROM live_channels")
    suspend fun deleteAll()

    /**
     * Replaces the whole table with what [chunks] yields, in one transaction: a download that breaks halfway throws, and the
     * old catalog stays as it was. Returns how many rows were stored.
     */
    @Transaction
    suspend fun replaceAll(chunks: Sequence<List<LiveChannelEntity>>): Int {
        deleteAll()
        var stored = 0
        for (chunk in chunks) {
            insertAll(chunk)
            stored += chunk.size
        }
        return stored
    }
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY position")
    fun pagingAll(): PagingSource<Int, MovieEntity>

    @Query("SELECT * FROM movies WHERE categoryId = :categoryId ORDER BY position")
    fun pagingByCategory(categoryId: String): PagingSource<Int, MovieEntity>

    @Query("SELECT COUNT(*) FROM movies")
    fun observeCount(): Flow<Int>

    @Query("SELECT categoryId, COUNT(*) AS count FROM movies GROUP BY categoryId")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    /** Build the query with [com.androbito.tilfaz.core.database.CatalogSearch.query]. */
    @RawQuery(observedEntities = [MovieEntity::class])
    fun search(query: SupportSQLiteQuery): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE categoryId = :categoryId AND id != :excludeId ORDER BY position LIMIT :limit")
    fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies ORDER BY addedAtMillis DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    fun observeByIds(ids: List<Int>): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun byId(id: Int): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<MovieEntity>)

    @Query("DELETE FROM movies")
    suspend fun deleteAll()

    /**
     * Replaces the whole table with what [chunks] yields, in one transaction: a download that breaks halfway throws, and the
     * old catalog stays as it was. Returns how many rows were stored.
     */
    @Transaction
    suspend fun replaceAll(chunks: Sequence<List<MovieEntity>>): Int {
        deleteAll()
        var stored = 0
        for (chunk in chunks) {
            insertAll(chunk)
            stored += chunk.size
        }
        return stored
    }
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series ORDER BY position")
    fun pagingAll(): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM series WHERE categoryId = :categoryId ORDER BY position")
    fun pagingByCategory(categoryId: String): PagingSource<Int, SeriesEntity>

    @Query("SELECT COUNT(*) FROM series")
    fun observeCount(): Flow<Int>

    @Query("SELECT categoryId, COUNT(*) AS count FROM series GROUP BY categoryId")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    /** Build the query with [com.androbito.tilfaz.core.database.CatalogSearch.query]. */
    @RawQuery(observedEntities = [SeriesEntity::class])
    fun search(query: SupportSQLiteQuery): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE categoryId = :categoryId AND id != :excludeId ORDER BY position LIMIT :limit")
    fun observeRelated(categoryId: String, excludeId: Int, limit: Int): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series ORDER BY lastModifiedMillis DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id IN (:ids)")
    fun observeByIds(ids: List<Int>): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun byId(id: Int): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<SeriesEntity>)

    @Query("DELETE FROM series")
    suspend fun deleteAll()

    /**
     * Replaces the whole table with what [chunks] yields, in one transaction: a download that breaks halfway throws, and the
     * old catalog stays as it was. Returns how many rows were stored.
     */
    @Transaction
    suspend fun replaceAll(chunks: Sequence<List<SeriesEntity>>): Int {
        deleteAll()
        var stored = 0
        for (chunk in chunks) {
            insertAll(chunk)
            stored += chunk.size
        }
        return stored
    }
}

@Dao
interface SyncStatusDao {
    @Query("SELECT * FROM sync_status WHERE contentType = :type")
    fun observe(type: String): Flow<SyncStatusEntity?>

    @Upsert
    suspend fun upsert(status: SyncStatusEntity)

    @Query("DELETE FROM sync_status")
    suspend fun deleteAll()
}
