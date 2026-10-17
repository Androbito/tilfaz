package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.SyncStatus
import java.time.Duration
import java.time.Instant

/**
 * When the cached catalogs are downloaded again by themselves: never downloaded, or saved a day ago or more. Opening the
 * app and its sections always shows what is stored; this only decides what to refresh behind it.
 */
object CatalogRefreshPolicy {
    val MAX_AGE: Duration = Duration.ofHours(24)

    fun due(statuses: List<SyncStatus>, now: Instant): Set<ContentType> = statuses
        .filter { status -> !status.isSyncing && (status.lastSyncedAt?.let { Duration.between(it, now) >= MAX_AGE } ?: true) }
        .map { it.contentType }
        .toSet()
}
