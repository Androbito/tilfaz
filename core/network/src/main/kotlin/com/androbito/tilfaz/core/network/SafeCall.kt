package com.androbito.tilfaz.core.network

import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Runs a network call and translates transport and parsing failures into [DomainException]s,
 * so repositories and ViewModels never see Retrofit or OkHttp types.
 */
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e.toDomainException())
}

fun Throwable.toDomainException(): DomainException = when (this) {
    is DomainException -> this
    is NotSignedInException -> DomainException(DomainError.NotSignedIn, this)
    is HttpException -> DomainException(
        when (code()) {
            401, 403 -> DomainError.Unauthorized
            in 500..599 -> DomainError.Server(code())
            else -> DomainError.BadResponse("HTTP ${code()}")
        },
        this
    )
    is SerializationException -> DomainException(DomainError.BadResponse(message?.take(200)), this)
    is IOException -> DomainException(DomainError.Network, this)
    else -> DomainException(DomainError.Unknown(this), this)
}
