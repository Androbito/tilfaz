package com.androbito.tilfaz.core.model

/** Failures the UI knows how to talk about. Technical exceptions are translated to these in the data layer. */
sealed interface DomainError {
    data object Network : DomainError
    data object Unauthorized : DomainError
    data object AccountExpired : DomainError
    data object NotSignedIn : DomainError
    data class Server(val statusCode: Int) : DomainError
    data class BadResponse(val detail: String?) : DomainError
    data class Unknown(val cause: Throwable?) : DomainError
}

class DomainException(val error: DomainError, cause: Throwable? = null) : Exception(error.toString(), cause)

fun Throwable.toDomainError(): DomainError = (this as? DomainException)?.error ?: DomainError.Unknown(this)

fun <T> Result<T>.domainErrorOrNull(): DomainError? = exceptionOrNull()?.toDomainError()
