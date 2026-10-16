package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.androbito.tilfaz.core.domain.usecase.SignInUseCase.Failure
import com.androbito.tilfaz.core.domain.usecase.SignInUseCase.Outcome
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.testing.fake.FakeAccountRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignInUseCaseTest {
    private val repository = FakeAccountRepository()
    private val signIn = SignInUseCase(repository)

    @Test
    fun `normalises the server before calling the repository`() = runTest {
        val outcome = signIn("tv.example.com:8080/", " ayoub ", "s3cret")

        assertThat(outcome).isEqualTo(Outcome.Success(Fixtures.activeStatus))
        assertThat(repository.signInAttempts).isEqualTo(listOf(Account("http://tv.example.com:8080", "ayoub", "s3cret")))
    }

    @Test
    fun `invalid server never reaches the network`() = runTest {
        val outcome = signIn("", "ayoub", "s3cret")

        assertThat(outcome).isEqualTo(Outcome.Failed(Failure.InvalidServer(ServerUrlNormalizer.Reason.EMPTY)))
        assertThat(repository.signInAttempts).isEqualTo(emptyList())
    }

    @Test
    fun `empty username and password are rejected locally`() = runTest {
        assertThat(signIn("tv.example.com", "  ", "x")).isEqualTo(Outcome.Failed(Failure.EmptyUsername))
        assertThat(signIn("tv.example.com", "x", "")).isEqualTo(Outcome.Failed(Failure.EmptyPassword))
    }

    @Test
    fun `remote failure is surfaced with its cause`() = runTest {
        repository.signInResult = Result.failure(DomainException(DomainError.Unauthorized))

        val outcome = signIn("tv.example.com", "ayoub", "wrong")

        assertThat(outcome).isInstanceOf<Outcome.Failed>()
        val failure = (outcome as Outcome.Failed).failure as Failure.Remote
        assertThat((failure.cause as DomainException).error).isEqualTo(DomainError.Unauthorized)
    }
}
