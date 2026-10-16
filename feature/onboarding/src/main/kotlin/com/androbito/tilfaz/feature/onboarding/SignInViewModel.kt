package com.androbito.tilfaz.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.usecase.ServerUrlNormalizer
import com.androbito.tilfaz.core.domain.usecase.SignInUseCase
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.toDomainError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val fieldError: FieldError? = null,
    val remoteError: RemoteError? = null,
    val signedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = !isSubmitting && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

sealed interface FieldError {
    data object ServerEmpty : FieldError
    data object ServerInvalid : FieldError
    data object ServerScheme : FieldError
    data object Username : FieldError
    data object Password : FieldError
}

enum class RemoteError { UNAUTHORIZED, EXPIRED, NETWORK, BAD_RESPONSE, UNKNOWN }

sealed interface SignInEvent {
    data class ServerChanged(val value: String) : SignInEvent
    data class UsernameChanged(val value: String) : SignInEvent
    data class PasswordChanged(val value: String) : SignInEvent
    data object Submit : SignInEvent
    data object SignedInConsumed : SignInEvent
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signIn: SignInUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.ServerChanged -> _state.update { it.copy(serverUrl = event.value, fieldError = null, remoteError = null) }
            is SignInEvent.UsernameChanged -> _state.update { it.copy(username = event.value, fieldError = null, remoteError = null) }
            is SignInEvent.PasswordChanged -> _state.update { it.copy(password = event.value, fieldError = null, remoteError = null) }
            SignInEvent.Submit -> submit()
            SignInEvent.SignedInConsumed -> _state.update { it.copy(signedIn = false) }
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, fieldError = null, remoteError = null) }
        viewModelScope.launch {
            when (val outcome = signIn(current.serverUrl, current.username, current.password)) {
                is SignInUseCase.Outcome.Success -> _state.update { it.copy(isSubmitting = false, signedIn = true) }
                is SignInUseCase.Outcome.Failed -> _state.update { it.copy(isSubmitting = false).withFailure(outcome.failure) }
            }
        }
    }

    private fun SignInUiState.withFailure(failure: SignInUseCase.Failure): SignInUiState = when (failure) {
        is SignInUseCase.Failure.InvalidServer -> copy(
            fieldError = when (failure.reason) {
                ServerUrlNormalizer.Reason.EMPTY -> FieldError.ServerEmpty
                ServerUrlNormalizer.Reason.UNSUPPORTED_SCHEME -> FieldError.ServerScheme
                ServerUrlNormalizer.Reason.UNPARSEABLE, ServerUrlNormalizer.Reason.MISSING_HOST -> FieldError.ServerInvalid
            },
        )
        SignInUseCase.Failure.EmptyUsername -> copy(fieldError = FieldError.Username)
        SignInUseCase.Failure.EmptyPassword -> copy(fieldError = FieldError.Password)
        is SignInUseCase.Failure.Remote -> copy(
            remoteError = when (failure.cause.toDomainError()) {
                DomainError.Unauthorized, DomainError.NotSignedIn -> RemoteError.UNAUTHORIZED
                DomainError.AccountExpired -> RemoteError.EXPIRED
                DomainError.Network -> RemoteError.NETWORK
                is DomainError.BadResponse, is DomainError.Server -> RemoteError.BAD_RESPONSE
                is DomainError.Unknown -> RemoteError.UNKNOWN
            },
        )
    }
}
