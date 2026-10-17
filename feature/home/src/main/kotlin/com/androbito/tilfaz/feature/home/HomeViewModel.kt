package com.androbito.tilfaz.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.usecase.CatalogRefreshPolicy
import com.androbito.tilfaz.core.domain.usecase.SyncCatalogUseCase
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val accountName: String = "",
    val accountStatus: AccountStatus? = null,
    val syncStatuses: Map<ContentType, SyncStatus> = emptyMap(),
    val syncErrors: Map<ContentType, DomainError> = emptyMap()
) {
    fun status(type: ContentType): SyncStatus = syncStatuses[type] ?: SyncStatus(type, null, 0)
}

sealed interface HomeEvent {
    data class Refresh(val type: ContentType) : HomeEvent
    data object RefreshAll : HomeEvent

    /** Home came to the front: anything never downloaded, or a day old, is downloaded again behind what is shown. */
    data object Shown : HomeEvent
    data class ErrorShown(val type: ContentType) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val liveRepository: LiveRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository,
    private val syncCatalog: SyncCatalogUseCase,
    private val clock: Clock
) : ViewModel() {

    private val errors = MutableStateFlow<Map<ContentType, DomainError>>(emptyMap())
    private val repositories get() = listOf(liveRepository, vodRepository, seriesRepository)

    val state: StateFlow<HomeUiState> = combine(
        accountRepository.observeAccount(),
        accountRepository.observeStatus(),
        combine(liveRepository.observeSyncStatus(), vodRepository.observeSyncStatus(), seriesRepository.observeSyncStatus()) { l, v, s -> listOf(l, v, s) },
        errors
    ) { account, status, syncs, errors ->
        HomeUiState(
            accountName = account?.displayName.orEmpty(),
            accountStatus = status,
            syncStatuses = syncs.associateBy { it.contentType },
            syncErrors = errors
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> viewModelScope.launch { sync(setOf(event.type)) }
            HomeEvent.Shown -> viewModelScope.launch {
                // What is stored is shown at once; only what is due is downloaded, and a failed download is not recorded, so it is retried.
                val due = CatalogRefreshPolicy.due(repositories.map { it.observeSyncStatus().first() }, clock.instant())
                if (due.isNotEmpty()) sync(due)
            }
            HomeEvent.RefreshAll -> viewModelScope.launch { sync(ContentType.entries.toSet()) }
            is HomeEvent.ErrorShown -> errors.update { it - event.type }
        }
    }

    private suspend fun sync(types: Set<ContentType>) {
        errors.update { it - types }
        val report = syncCatalog(types)
        errors.update { it + report.failures }
    }
}
