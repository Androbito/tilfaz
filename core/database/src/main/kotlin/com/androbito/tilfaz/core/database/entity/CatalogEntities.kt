package com.androbito.tilfaz.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Category ids are only unique within a content type, hence the composite key. */
@Entity(tableName = "categories", primaryKeys = ["id", "type"])
data class CategoryEntity(
    val id: String,
    val type: String,
    val name: String,
    val parentId: String?,
    val position: Int
)

@Entity(tableName = "live_channels", indices = [Index("categoryId"), Index("name")])
data class LiveChannelEntity(
    @PrimaryKey val id: Int,
    val number: Int?,
    val name: String,
    val logoUrl: String?,
    val categoryId: String?,
    val epgChannelId: String?,
    val hasArchive: Boolean,
    val archiveDays: Int,
    val position: Int
)

@Entity(tableName = "movies", indices = [Index("categoryId"), Index("name"), Index("addedAtMillis")])
data class MovieEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val posterUrl: String?,
    val categoryId: String?,
    val ratingOutOfTen: Double?,
    val year: Int?,
    val addedAtMillis: Long?,
    val containerExtension: String,
    val position: Int
)

@Entity(tableName = "series", indices = [Index("categoryId"), Index("name"), Index("lastModifiedMillis")])
data class SeriesEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val posterUrl: String?,
    val categoryId: String?,
    val ratingOutOfTen: Double?,
    val year: Int?,
    val lastModifiedMillis: Long?,
    val plot: String?,
    val genre: String?,
    val position: Int
)

@Entity(tableName = "sync_status")
data class SyncStatusEntity(
    @PrimaryKey val contentType: String,
    val lastSyncedAtMillis: Long?,
    val itemCount: Int
)
