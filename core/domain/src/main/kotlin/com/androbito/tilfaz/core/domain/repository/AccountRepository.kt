package com.androbito.tilfaz.core.domain.repository

import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccount(): Flow<Account?>
    fun observeStatus(): Flow<AccountStatus?>

    /** Authenticates against the server and, on success, stores the account as the active one. */
    suspend fun signIn(account: Account): Result<AccountStatus>
    suspend fun refreshStatus(): Result<AccountStatus>
    suspend fun signOut()
}
