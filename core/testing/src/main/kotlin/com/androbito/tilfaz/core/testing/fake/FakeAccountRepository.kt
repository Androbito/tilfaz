package com.androbito.tilfaz.core.testing.fake

import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountRepository(initial: Account? = null) : AccountRepository {
    val account = MutableStateFlow(initial)
    val status = MutableStateFlow<AccountStatus?>(null)
    var signInResult: Result<AccountStatus> = Result.success(Fixtures.activeStatus)
    val signInAttempts = mutableListOf<Account>()
    var signedOut = false

    override fun observeAccount(): Flow<Account?> = account
    override fun observeStatus(): Flow<AccountStatus?> = status

    override suspend fun signIn(account: Account): Result<AccountStatus> {
        signInAttempts += account
        signInResult.onSuccess {
            this.account.value = account
            status.value = it
        }
        return signInResult
    }

    override suspend fun refreshStatus(): Result<AccountStatus> = signInResult.onSuccess { status.value = it }

    override suspend fun signOut() {
        signedOut = true
        account.value = null
        status.value = null
    }
}
