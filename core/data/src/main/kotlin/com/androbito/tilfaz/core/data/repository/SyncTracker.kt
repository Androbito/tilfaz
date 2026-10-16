package com.androbito.tilfaz.core.data.repository

import com.androbito.tilfaz.core.data.mapper.toDomain
import com.androbito.tilfaz.core.database.dao.SyncStatusDao
import com.androbito.tilfaz.core.database.entity.SyncStatusEntity
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.SyncStatus
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** Shared by the three catalog repositories: persisted "last synced" plus a transient "syncing now" flag. */
internal class SyncTracker(
    private val type: ContentType,
    private val dao: SyncStatusDao,
    private val clock: Clock
) {
    private val syncing = MutableStateFlow(false)

    fun observe(): Flow<SyncStatus> = combine(dao.observe(type.name), syncing) { entity, isSyncing -> entity.toDomain(type, isSyncing) }

    suspend fun <T> track(block: suspend () -> T): T {
        syncing.value = true
        try {
            return block()
        } finally {
            syncing.value = false
        }
    }

    suspend fun markSynced(itemCount: Int) {
        dao.upsert(SyncStatusEntity(contentType = type.name, lastSyncedAtMillis = clock.millis(), itemCount = itemCount))
    }
}
