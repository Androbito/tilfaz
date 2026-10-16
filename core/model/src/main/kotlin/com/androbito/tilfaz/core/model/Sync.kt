package com.androbito.tilfaz.core.model

import java.time.Instant

data class SyncStatus(
    val contentType: ContentType,
    val lastSyncedAt: Instant?,
    val itemCount: Int,
    val isSyncing: Boolean = false,
) {
    val hasData: Boolean get() = itemCount > 0
}
