package com.androbito.tilfaz.core.model

data class LiveChannel(
    val id: Int,
    val number: Int?,
    val name: String,
    val logoUrl: String?,
    val categoryId: String?,
    val epgChannelId: String?,
    val hasArchive: Boolean = false,
    val archiveDays: Int = 0,
)
