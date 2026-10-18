package com.androbito.tilfaz.core.database.di

import android.content.Context
import androidx.room.Room
import com.androbito.tilfaz.core.database.Migrations
import com.androbito.tilfaz.core.database.TilfazDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TilfazDatabase =
        Room.databaseBuilder(context, TilfazDatabase::class.java, "tilfaz.db")
            // Favourites and progress share this file, so a schema change is always a migration, never a wipe.
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun provideCategoryDao(db: TilfazDatabase) = db.categoryDao()

    @Provides fun provideLiveChannelDao(db: TilfazDatabase) = db.liveChannelDao()

    @Provides fun provideMovieDao(db: TilfazDatabase) = db.movieDao()

    @Provides fun provideSeriesDao(db: TilfazDatabase) = db.seriesDao()

    @Provides fun provideSyncStatusDao(db: TilfazDatabase) = db.syncStatusDao()

    @Provides fun provideFavoriteDao(db: TilfazDatabase) = db.favoriteDao()

    @Provides fun provideWatchlistDao(db: TilfazDatabase) = db.watchlistDao()

    @Provides fun provideWatchProgressDao(db: TilfazDatabase) = db.watchProgressDao()

    @Provides fun provideRecentChannelDao(db: TilfazDatabase) = db.recentChannelDao()
}
