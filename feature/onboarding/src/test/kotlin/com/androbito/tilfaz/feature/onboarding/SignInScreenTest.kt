package com.androbito.tilfaz.feature.onboarding

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The smallest TV we support is 960x540dp (1080p at xhdpi, 2px per dp): the whole form must fit without scrolling. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class SignInScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun submitButtonStaysInsideTheTvSafeArea() {
        val filled = SignInUiState(serverUrl = "http://example.com:8080", username = "user", password = "secret")
        compose.setContent { TilfazTheme { SignInScreen(state = filled, onEvent = {}) } }

        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val button = compose.onNode(hasText("Sign in") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val safeMargin = with(compose.density) { Dimens.ScreenPadding.toPx() }

        assertThat(button.bottom).isLessThanOrEqualTo(screen.bottom - safeMargin)
        assertThat(button.height).isGreaterThanOrEqualTo(MIN_BUTTON_HEIGHT_PX)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun dpadDownMovesFocusThroughTheFormFields() {
        compose.setContent { TilfazTheme { SignInScreen(state = SignInUiState(), onEvent = {}) } }
        val fields = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))

        fields[0].assertIsFocused()
        fields[0].performKeyInput { pressKey(Key.DirectionDown) }
        fields[1].assertIsFocused()
        fields[1].performKeyInput { pressKey(Key.DirectionDown) }
        fields[2].assertIsFocused()
    }

    private fun signInLabelColor(): Color {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Sign in", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.first().layoutInput.style.color
    }

    private fun showFilledForm() {
        val filled = SignInUiState(serverUrl = "http://example.com:8080", username = "user", password = "secret")
        compose.setContent { TilfazTheme { SignInScreen(state = filled, onEvent = {}) } }
    }

    @Test
    fun serverFieldHasFocusOnLaunchEvenWhenTheFormIsPrefilled() {
        showFilledForm()
        compose.waitForIdle()

        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))[0].assertIsFocused()
    }

    @Test
    fun signInLabelIsWhiteWhenTheButtonIsEnabled() {
        showFilledForm()

        assertThat(signInLabelColor()).isEqualTo(TilfazColors.OnAccent)
    }

    @Test
    fun signInLabelStaysWhiteWhenTheButtonIsFocused() {
        showFilledForm()
        compose.onNode(hasText("Sign in") and hasClickAction()).requestFocus()
        compose.waitForIdle()

        assertThat(signInLabelColor()).isEqualTo(TilfazColors.OnAccent)
    }

    private companion object {
        const val MIN_BUTTON_HEIGHT_PX = 80f // 40dp: a button with a 18sp label and padding is never shorter
    }
}
