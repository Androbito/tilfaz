package com.androbito.tilfaz.core.database.entity

import androidx.room.ColumnInfo
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

@Entity(tableName = "live_channels", indices = [Index("categoryId", "position"), Index("position")])
data class LiveChannelEntity(
    @PrimaryKey val id: Int,
    val number: Int?,
    val name: String,
    /** [com.androbito.tilfaz.core.model.TitleText.searchKey] of the name: what a search compares against. */
    @ColumnInfo(defaultValue = "''") val nameNormalized: String = "",
    val logoUrl: String?,
    val categoryId: String?,
    val epgChannelId: String?,
    val hasArchive: Boolean,
    val archiveDays: Int,
    val position: Int
)

@Entity(
    tableName = "movies",
    indices = [
        Index("categoryId", "position"),
        Index("position"),
        // The whole catalog is listed newest first, then in provider order: this index is that order, so nothing is sorted at read time.
        Index(value = ["addedAtMillis", "position"], orders = [Index.Order.DESC, Index.Order.ASC])
    ]
)
data class MovieEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /** [com.androbito.tilfaz.core.model.TitleText.searchKey] of the name: what a search compares against. */
    @ColumnInfo(defaultValue = "''") val nameNormalized: String = "",
    val posterUrl: String?,
    val categoryId: String?,
    val ratingOutOfTen: Double?,
    val year: Int?,
    val addedAtMillis: Long?,
    val containerExtension: String,
    val position: Int
)

@Entity(
    tableName = "series",
    indices = [
        Index("categoryId", "position"),
        Index("position"),
        Index(value = ["lastModifiedMillis", "position"], orders = [Index.Order.DESC, Index.Order.ASC])
    ]
)
data class SeriesEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /** [com.androbito.tilfaz.core.model.TitleText.searchKey] of the name: what a search compares against. */
    @ColumnInfo(defaultValue = "''") val nameNormalized: String = "",
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
