package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    sealed interface Failure {
        data class InvalidServer(val reason: ServerUrlNormalizer.Reason) : Failure
        data object EmptyUsername : Failure
        data object EmptyPassword : Failure
        data class Remote(val cause: Throwable) : Failure
    }

    sealed interface Outcome {
        data class Success(val status: AccountStatus) : Outcome
        data class Failed(val failure: Failure) : Outcome
    }

    suspend operator fun invoke(serverUrl: String, username: String, password: String): Outcome {
        val server = when (val n = ServerUrlNormalizer.normalize(serverUrl)) {
            is ServerUrlNormalizer.Outcome.Invalid -> return Outcome.Failed(Failure.InvalidServer(n.reason))
            is ServerUrlNormalizer.Outcome.Valid -> n.url
        }
        val user = username.trim().ifEmpty { return Outcome.Failed(Failure.EmptyUsername) }
        val pass = password.trim().ifEmpty { return Outcome.Failed(Failure.EmptyPassword) }

        return accountRepository.signIn(Account(server, user, pass)).fold(
            onSuccess = { Outcome.Success(it) },
            onFailure = { Outcome.Failed(Failure.Remote(it)) },
        )
    }
}
