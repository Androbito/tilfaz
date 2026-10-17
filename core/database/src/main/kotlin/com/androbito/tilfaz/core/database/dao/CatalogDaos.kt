package com.androbito.tilfaz.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.androbito.tilfaz.core.database.entity.CategoryEntity
import com.androbito.tilfaz.core.database.entity.LiveChannelEntity
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.database.entity.SeriesEntity
import com.androbito.tilfaz.core.database.entity.SyncStatusEntity
import kotlinx.coroutines.flow.Flow

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
    fun observeAll(): Flow<List<LiveChannelEntity>>

    @Query("SELECT * FROM live_channels WHERE categoryId = :categoryId ORDER BY position")
    fun observeByCategory(categoryId: String): Flow<List<LiveChannelEntity>>

    @Query("SELECT * FROM live_channels WHERE id IN (:ids)")
    fun observeByIds(ids: List<Int>): Flow<List<LiveChannelEntity>>

    @Query("SELECT * FROM live_channels WHERE id = :id")
    suspend fun byId(id: Int): LiveChannelEntity?

    @Query("SELECT COUNT(*) FROM live_channels")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<LiveChannelEntity>)

    @Query("DELETE FROM live_channels")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(channels: List<LiveChannelEntity>) {
        deleteAll()
        insertAll(channels)
    }
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY position")
    fun observeAll(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE categoryId = :categoryId ORDER BY position")
    fun observeByCategory(categoryId: String): Flow<List<MovieEntity>>

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

    @Transaction
    suspend fun replaceAll(movies: List<MovieEntity>) {
        deleteAll()
        insertAll(movies)
    }
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series ORDER BY position")
    fun observeAll(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE categoryId = :categoryId ORDER BY position")
    fun observeByCategory(categoryId: String): Flow<List<SeriesEntity>>

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

    @Transaction
    suspend fun replaceAll(series: List<SeriesEntity>) {
        deleteAll()
        insertAll(series)
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
