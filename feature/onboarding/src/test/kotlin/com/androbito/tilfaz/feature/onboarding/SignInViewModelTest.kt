package com.androbito.tilfaz.feature.onboarding

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.domain.usecase.SignInUseCase
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.testing.fake.FakeAccountRepository
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAccountRepository()
    private val viewModel = SignInViewModel(SignInUseCase(repository))

    private fun fill() {
        viewModel.onEvent(SignInEvent.ServerChanged("tv.example.com:8080"))
        viewModel.onEvent(SignInEvent.UsernameChanged("ayoub"))
        viewModel.onEvent(SignInEvent.PasswordChanged("s3cret"))
    }

    @Test
    fun `submit is disabled until every field is filled`() {
        assertThat(viewModel.state.value.canSubmit).isFalse()
        fill()
        assertThat(viewModel.state.value.canSubmit).isTrue()
    }

    @Test
    fun `successful sign in raises the signedIn flag once`() = runTest {
        fill()
        viewModel.onEvent(SignInEvent.Submit)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertThat(state.signedIn).isTrue()
            assertThat(state.isSubmitting).isFalse()
        }
        viewModel.onEvent(SignInEvent.SignedInConsumed)
        assertThat(viewModel.state.value.signedIn).isFalse()
    }

    @Test
    fun `invalid server shows a field error and does not call the server`() {
        viewModel.onEvent(SignInEvent.ServerChanged("rtmp://x"))
        viewModel.onEvent(SignInEvent.UsernameChanged("a"))
        viewModel.onEvent(SignInEvent.PasswordChanged("b"))
        viewModel.onEvent(SignInEvent.Submit)

        assertThat(viewModel.state.value.fieldError).isEqualTo(FieldError.ServerScheme)
        assertThat(repository.signInAttempts).isEqualTo(emptyList())
    }

    @Test
    fun `remote errors are mapped to user facing categories`() {
        repository.signInResult = Result.failure(DomainException(DomainError.AccountExpired))
        fill()
        viewModel.onEvent(SignInEvent.Submit)
        assertThat(viewModel.state.value.remoteError).isEqualTo(RemoteError.EXPIRED)

        repository.signInResult = Result.failure(DomainException(DomainError.Network))
        viewModel.onEvent(SignInEvent.Submit)
        assertThat(viewModel.state.value.remoteError).isEqualTo(RemoteError.NETWORK)
    }

    @Test
    fun `editing a field clears previous errors`() {
        repository.signInResult = Result.failure(DomainException(DomainError.Unauthorized))
        fill()
        viewModel.onEvent(SignInEvent.Submit)
        assertThat(viewModel.state.value.remoteError).isEqualTo(RemoteError.UNAUTHORIZED)

        viewModel.onEvent(SignInEvent.PasswordChanged("better"))
        assertThat(viewModel.state.value.remoteError).isNull()
    }
}
