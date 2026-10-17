package com.androbito.tilfaz.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.BrandMark
import com.androbito.tilfaz.core.designsystem.component.TvTextField
import com.androbito.tilfaz.core.designsystem.component.tilfazButtonBorder
import com.androbito.tilfaz.core.designsystem.component.tilfazButtonColors
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import kotlinx.serialization.Serializable

@Serializable
data object SignInRoute

fun NavGraphBuilder.signInScreen(onSignedIn: () -> Unit) {
    composable<SignInRoute> {
        SignInRoute(onSignedIn = onSignedIn)
    }
}

@Composable
fun SignInRoute(onSignedIn: () -> Unit, viewModel: SignInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            viewModel.onEvent(SignInEvent.SignedInConsumed)
            onSignedIn()
        }
    }
    SignInScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun SignInScreen(state: SignInUiState, onEvent: (SignInEvent) -> Unit) {
    val serverFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { serverFocus.requestFocus() }

    Row(
        Modifier.fillMaxSize().padding(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            BrandMark("Tilfaz")
            Text(stringResource(R.string.signin_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.signin_subtitle), style = MaterialTheme.typography.bodyLarge, color = TilfazColors.OnSurfaceMuted)
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            TvTextField(
                value = state.serverUrl,
                onValueChange = { onEvent(SignInEvent.ServerChanged(it)) },
                label = stringResource(R.string.signin_server),
                placeholder = stringResource(R.string.signin_server_hint),
                keyboardType = KeyboardType.Uri,
                errorText = state.fieldError.serverMessage(),
                modifier = Modifier.focusRequester(serverFocus)
            )
            TvTextField(
                value = state.username,
                onValueChange = { onEvent(SignInEvent.UsernameChanged(it)) },
                label = stringResource(R.string.signin_username),
                errorText = if (state.fieldError == FieldError.Username) stringResource(R.string.signin_error_username) else null
            )
            TvTextField(
                value = state.password,
                onValueChange = { onEvent(SignInEvent.PasswordChanged(it)) },
                label = stringResource(R.string.signin_password),
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go,
                onImeAction = { if (state.canSubmit) onEvent(SignInEvent.Submit) },
                errorText = if (state.fieldError == FieldError.Password) stringResource(R.string.signin_error_password) else null
            )

            state.remoteError?.let { error ->
                Text(error.message(), style = MaterialTheme.typography.bodyLarge, color = TilfazColors.Danger)
            }

            Button(
                onClick = { onEvent(SignInEvent.Submit) },
                enabled = state.canSubmit,
                colors = tilfazButtonColors(),
                border = tilfazButtonBorder(),
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceS)
            ) {
                Text(if (state.isSubmitting) stringResource(R.string.signin_in_progress) else stringResource(R.string.signin_action))
            }
        }
    }
}

@Composable
private fun FieldError?.serverMessage(): String? = when (this) {
    FieldError.ServerEmpty -> stringResource(R.string.signin_error_server_empty)
    FieldError.ServerInvalid -> stringResource(R.string.signin_error_server_invalid)
    FieldError.ServerScheme -> stringResource(R.string.signin_error_server_scheme)
    else -> null
}

@Composable
private fun RemoteError.message(): String = when (this) {
    RemoteError.UNAUTHORIZED -> stringResource(R.string.signin_error_unauthorized)
    RemoteError.EXPIRED -> stringResource(R.string.signin_error_expired)
    RemoteError.NETWORK -> stringResource(R.string.signin_error_network)
    RemoteError.BAD_RESPONSE -> stringResource(R.string.signin_error_bad_response)
    RemoteError.UNKNOWN -> stringResource(R.string.signin_error_unknown)
}
