package com.androbito.tilfaz.core.data.repository

import com.androbito.tilfaz.core.data.di.IoDispatcher
import com.androbito.tilfaz.core.data.mapper.AuthMapper
import com.androbito.tilfaz.core.data.store.AccountStore
import com.androbito.tilfaz.core.data.store.ActiveAccountHolder
import com.androbito.tilfaz.core.database.TilfazDatabase
import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.network.XtreamApi
import com.androbito.tilfaz.core.network.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val api: XtreamApi,
    private val accountStore: AccountStore,
    private val accountHolder: ActiveAccountHolder,
    private val database: TilfazDatabase,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AccountRepository {

    override fun observeAccount(): Flow<Account?> = accountStore.account
    override fun observeStatus(): Flow<AccountStatus?> = accountStore.status

    override suspend fun signIn(account: Account): Result<AccountStatus> = withContext(io) {
        val previous = accountStore.account.first()
        accountHolder.withCandidate(account) { safeApiCall { api.authenticate() } }
            .mapCatching { AuthMapper.toStatus(it, clock.instant()) }
            .onSuccess { status ->
                if (previous != null && previous != account) database.clearAllTables()
                accountStore.save(account, status)
            }
    }

    override suspend fun refreshStatus(): Result<AccountStatus> = withContext(io) {
        safeApiCall { api.authenticate() }
            .mapCatching { AuthMapper.toStatus(it, clock.instant()) }
            .onSuccess { accountStore.saveStatus(it) }
    }

    override suspend fun signOut() = withContext(io) {
        accountStore.clear()
        database.clearAllTables()
    }
}
