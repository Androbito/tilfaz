package com.androbito.tilfaz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

/**
 * Decides the start destination once: stored account -> home, otherwise the sign-in screen.
 * Later sign-in/sign-out is handled by navigation, so this value never changes after the first emission
 * and the NavHost is never recreated underneath the user.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    accountRepository: AccountRepository
) : ViewModel() {

    enum class Session { Loading, SignedOut, SignedIn }

    val initialSession: StateFlow<Session> = accountRepository.observeAccount()
        .take(1)
        .map { if (it == null) Session.SignedOut else Session.SignedIn }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Session.Loading)
}
