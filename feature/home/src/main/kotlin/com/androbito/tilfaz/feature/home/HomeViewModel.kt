package com.androbito.tilfaz.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.usecase.SyncCatalogUseCase
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val accountName: String = "",
    val accountStatus: AccountStatus? = null,
    val syncStatuses: Map<ContentType, SyncStatus> = emptyMap(),
    val syncErrors: Map<ContentType, DomainError> = emptyMap(),
) {
    fun status(type: ContentType): SyncStatus = syncStatuses[type] ?: SyncStatus(type, null, 0)
}

sealed interface HomeEvent {
    data class Refresh(val type: ContentType) : HomeEvent
    data object RefreshAll : HomeEvent
    data class ErrorShown(val type: ContentType) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    liveRepository: LiveRepository,
    vodRepository: VodRepository,
    seriesRepository: SeriesRepository,
    private val syncCatalog: SyncCatalogUseCase,
) : ViewModel() {

    private val errors = MutableStateFlow<Map<ContentType, DomainError>>(emptyMap())

    val state: StateFlow<HomeUiState> = combine(
        accountRepository.observeAccount(),
        accountRepository.observeStatus(),
        combine(liveRepository.observeSyncStatus(), vodRepository.observeSyncStatus(), seriesRepository.observeSyncStatus()) { l, v, s -> listOf(l, v, s) },
        errors,
    ) { account, status, syncs, errors ->
        HomeUiState(
            accountName = account?.displayName.orEmpty(),
            accountStatus = status,
            syncStatuses = syncs.associateBy { it.contentType },
            syncErrors = errors,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // First launch after sign-in: nothing is cached yet, fetch everything without waiting for a click.
        viewModelScope.launch {
            val statuses = listOf(liveRepository, vodRepository, seriesRepository).map { it.observeSyncStatus().first() }
            val missing = statuses.filter { !it.hasData && !it.isSyncing }.map { it.contentType }.toSet()
            if (missing.isNotEmpty()) sync(missing)
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> viewModelScope.launch { sync(setOf(event.type)) }
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
