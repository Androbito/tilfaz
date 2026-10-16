package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.toDomainError
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Refreshes one or all catalogs in parallel and reports the outcome per content type,
 * so the home screen can show "Live updated, Movies failed: network" instead of one global error.
 */
class SyncCatalogUseCase @Inject constructor(
    private val liveRepository: LiveRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository
) {
    data class Report(val failures: Map<ContentType, DomainError>) {
        val isFullSuccess: Boolean get() = failures.isEmpty()
    }

    suspend operator fun invoke(types: Set<ContentType> = ContentType.entries.toSet()): Report = coroutineScope {
        val jobs = types.map { type ->
            type to async {
                when (type) {
                    ContentType.LIVE -> liveRepository.refresh()
                    ContentType.VOD -> vodRepository.refresh()
                    ContentType.SERIES -> seriesRepository.refresh()
                }
            }
        }
        val failures = jobs.mapNotNull { (type, job) ->
            job.await().exceptionOrNull()?.let { type to it.toDomainError() }
        }.toMap()
        Report(failures)
    }
}
