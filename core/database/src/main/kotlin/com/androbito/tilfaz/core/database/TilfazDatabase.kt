package com.androbito.tilfaz.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androbito.tilfaz.core.database.dao.CategoryDao
import com.androbito.tilfaz.core.database.dao.FavoriteDao
import com.androbito.tilfaz.core.database.dao.LiveChannelDao
import com.androbito.tilfaz.core.database.dao.MovieDao
import com.androbito.tilfaz.core.database.dao.RecentChannelDao
import com.androbito.tilfaz.core.database.dao.SeriesDao
import com.androbito.tilfaz.core.database.dao.SyncStatusDao
import com.androbito.tilfaz.core.database.dao.WatchProgressDao
import com.androbito.tilfaz.core.database.dao.WatchlistDao
import com.androbito.tilfaz.core.database.entity.CategoryEntity
import com.androbito.tilfaz.core.database.entity.FavoriteEntity
import com.androbito.tilfaz.core.database.entity.LiveChannelEntity
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.database.entity.RecentChannelEntity
import com.androbito.tilfaz.core.database.entity.SeriesEntity
import com.androbito.tilfaz.core.database.entity.SyncStatusEntity
import com.androbito.tilfaz.core.database.entity.WatchProgressEntity
import com.androbito.tilfaz.core.database.entity.WatchlistEntity

@Database(
    entities = [
        CategoryEntity::class,
        LiveChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        SyncStatusEntity::class,
        FavoriteEntity::class,
        WatchProgressEntity::class,
        WatchlistEntity::class,
        RecentChannelEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class TilfazDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun liveChannelDao(): LiveChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun syncStatusDao(): SyncStatusDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun recentChannelDao(): RecentChannelDao

    /** Wipes catalog and sync data (account sign-out). User data such as favourites is cleared separately on purpose. */
    suspend fun clearCatalog() {
        liveChannelDao().deleteAll()
        movieDao().deleteAll()
        seriesDao().deleteAll()
        categoryDao().deleteByType("LIVE")
        categoryDao().deleteByType("VOD")
        categoryDao().deleteByType("SERIES")
        syncStatusDao().deleteAll()
    }
}
