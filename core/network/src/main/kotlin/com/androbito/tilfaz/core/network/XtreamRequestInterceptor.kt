package com.androbito.tilfaz.core.network

import com.androbito.tilfaz.core.model.Account
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Supplies the account whose server every request must target. Implemented in the data layer. */
fun interface ActiveAccountProvider {
    fun current(): Account?
}

/** Supplies the user agent configured in playback settings; many providers whitelist specific agents. */
fun interface UserAgentProvider {
    fun userAgent(): String
}

class NotSignedInException : IOException("No active Xtream account")

@Singleton
class XtreamRequestInterceptor @Inject constructor(
    private val accountProvider: ActiveAccountProvider,
    private val userAgentProvider: UserAgentProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val account = accountProvider.current() ?: throw NotSignedInException()
        val server = account.serverUrl.toHttpUrlOrNull() ?: throw IOException("Invalid server URL: ${account.serverUrl}")

        val url = original.url.newBuilder()
            .scheme(server.scheme)
            .host(server.host)
            .port(server.port)
            .setQueryParameter("username", account.username)
            .setQueryParameter("password", account.password)
            .build()

        val request = original.newBuilder()
            .url(url)
            .header("User-Agent", userAgentProvider.userAgent())
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
