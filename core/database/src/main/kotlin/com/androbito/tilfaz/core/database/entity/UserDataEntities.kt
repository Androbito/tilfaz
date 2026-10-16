package com.androbito.tilfaz.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorites", indices = [Index("type")])
data class FavoriteEntity(
    @PrimaryKey val key: String,
    val type: String,
    val addedAtMillis: Long
)

@Entity(tableName = "watch_progress", indices = [Index("type"), Index("updatedAtMillis")])
data class WatchProgressEntity(
    @PrimaryKey val playableKey: String,
    val type: String,
    val title: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMillis: Long,
    val reference: String
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey val channelId: Int,
    val viewedAtMillis: Long
)
