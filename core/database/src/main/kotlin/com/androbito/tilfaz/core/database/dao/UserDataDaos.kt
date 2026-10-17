package com.androbito.tilfaz.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.androbito.tilfaz.core.database.entity.FavoriteEntity
import com.androbito.tilfaz.core.database.entity.RecentChannelEntity
import com.androbito.tilfaz.core.database.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT `key` FROM favorites WHERE type = :type ORDER BY addedAtMillis DESC")
    fun observeKeys(type: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE `key` = :key)")
    suspend fun exists(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE `key` = :key")
    suspend fun delete(key: String)
}

@Dao
interface WatchProgressDao {
    /** Finished items are filtered here, before the limit, so they cannot push unfinished ones out of the list. */
    @Query(
        "SELECT * FROM watch_progress WHERE type = :type AND (durationMs <= 0 OR positionMs < :finishedFraction * durationMs) " +
            "ORDER BY updatedAtMillis DESC LIMIT :limit"
    )
    fun observeUnfinishedByType(type: String, finishedFraction: Double, limit: Int): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE playableKey LIKE :prefix || '%' ORDER BY updatedAtMillis DESC")
    fun observeByKeyPrefix(prefix: String): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE playableKey = :key")
    suspend fun byKey(key: String): WatchProgressEntity?

    @Upsert
    suspend fun upsert(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE playableKey = :key")
    suspend fun delete(key: String)
}

@Dao
interface RecentChannelDao {
    @Query("SELECT * FROM recent_channels ORDER BY viewedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentChannelEntity>>

    @Upsert
    suspend fun upsert(entry: RecentChannelEntity)

    @Query("DELETE FROM recent_channels WHERE channelId NOT IN (SELECT channelId FROM recent_channels ORDER BY viewedAtMillis DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}
