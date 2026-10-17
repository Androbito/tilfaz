package com.androbito.tilfaz.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.domain.repository.PlaybackSettingsRepository
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.model.BufferProfile
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: PlaybackSettings = PlaybackSettings(),
    val account: Account? = null,
    val status: AccountStatus? = null,
    val isRefreshingStatus: Boolean = false,
    val confirmSignOut: Boolean = false,
    val signedOut: Boolean = false
)

sealed interface SettingsEvent {
    data class SetLiveFormat(val format: StreamFormat) : SettingsEvent
    data class SetBuffer(val profile: BufferProfile) : SettingsEvent
    data class SetUserAgent(val value: String) : SettingsEvent
    data object ResetUserAgent : SettingsEvent
    data class SetResume(val enabled: Boolean) : SettingsEvent
    data class SetAudioLanguage(val language: String?) : SettingsEvent
    data class SetSubtitleLanguage(val language: String?) : SettingsEvent
    data class SetSubtitleSize(val size: SubtitleSize) : SettingsEvent
    data class SetSubtitleColor(val color: SubtitleColor) : SettingsEvent
    data class SetSubtitleBackground(val background: SubtitleBackground) : SettingsEvent
    data object RefreshStatus : SettingsEvent
    data object SignOutRequested : SettingsEvent
    data object SignOutCancelled : SettingsEvent
    data object SignOutConfirmed : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: PlaybackSettingsRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    private data class Transient(val refreshing: Boolean = false, val confirmSignOut: Boolean = false, val signedOut: Boolean = false)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.observe(),
        accountRepository.observeAccount(),
        accountRepository.observeStatus(),
        transient
    ) { settings, account, status, t ->
        SettingsUiState(settings, account, status, t.refreshing, t.confirmSignOut, t.signedOut)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetLiveFormat -> update { it.copy(liveFormat = event.format) }
            is SettingsEvent.SetBuffer -> update { it.copy(bufferProfile = event.profile) }
            is SettingsEvent.SetUserAgent -> update { it.copy(userAgent = event.value.trim().ifEmpty { PlaybackSettings.DEFAULT_USER_AGENT }) }
            SettingsEvent.ResetUserAgent -> update { it.copy(userAgent = PlaybackSettings.DEFAULT_USER_AGENT) }
            is SettingsEvent.SetResume -> update { it.copy(resumePlayback = event.enabled) }
            is SettingsEvent.SetAudioLanguage -> update { it.copy(audioLanguage = event.language) }
            is SettingsEvent.SetSubtitleLanguage -> update { it.copy(subtitleLanguage = event.language) }
            is SettingsEvent.SetSubtitleSize -> update { it.copy(subtitleStyle = it.subtitleStyle.copy(size = event.size)) }
            is SettingsEvent.SetSubtitleColor -> update { it.copy(subtitleStyle = it.subtitleStyle.copy(textColor = event.color)) }
            is SettingsEvent.SetSubtitleBackground -> update { it.copy(subtitleStyle = it.subtitleStyle.copy(background = event.background)) }
            SettingsEvent.RefreshStatus -> viewModelScope.launch {
                transient.value = transient.value.copy(refreshing = true)
                accountRepository.refreshStatus()
                transient.value = transient.value.copy(refreshing = false)
            }
            SettingsEvent.SignOutRequested -> transient.value = transient.value.copy(confirmSignOut = true)
            SettingsEvent.SignOutCancelled -> transient.value = transient.value.copy(confirmSignOut = false)
            SettingsEvent.SignOutConfirmed -> viewModelScope.launch {
                accountRepository.signOut()
                transient.value = transient.value.copy(confirmSignOut = false, signedOut = true)
            }
        }
    }

    private fun update(transform: (PlaybackSettings) -> PlaybackSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
