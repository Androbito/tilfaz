package com.androbito.tilfaz.core.data.store

import com.androbito.tilfaz.core.data.di.ApplicationScope
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.network.ActiveAccountProvider
import com.androbito.tilfaz.core.network.UserAgentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory mirror of the stored account and user agent, so OkHttp interceptors (synchronous, on their own threads)
 * can read them without touching DataStore. Also lets sign-in test a candidate account before it is persisted.
 */
@Singleton
class ActiveAccountHolder @Inject constructor(
    accountStore: AccountStore,
    settingsStore: PlaybackSettingsStore,
    @ApplicationScope scope: CoroutineScope,
) : ActiveAccountProvider, UserAgentProvider {

    private val stored = MutableStateFlow<Optional<Account>?>(null)
    private val agent = MutableStateFlow(PlaybackSettings.DEFAULT_USER_AGENT)

    @Volatile
    private var candidate: Account? = null

    init {
        accountStore.account.onEach { stored.value = Optional.ofNullable(it) }.launchIn(scope)
        settingsStore.settings.onEach { agent.value = it.userAgent }.launchIn(scope)
    }

    override fun current(): Account? = candidate ?: awaitStored()

    override fun userAgent(): String = agent.value

    /** Runs [block] with [account] as the active account, without persisting it. */
    suspend fun <T> withCandidate(account: Account, block: suspend () -> T): T {
        candidate = account
        try {
            return block()
        } finally {
            candidate = null
        }
    }

    private fun awaitStored(): Account? = runBlocking {
        withTimeoutOrNull(LOAD_TIMEOUT_MS) { stored.filterNotNull().first() }?.orElse(null)
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 5_000L
    }
}
