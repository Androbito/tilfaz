package com.androbito.tilfaz.core.designsystem

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.designsystem.component.TvTextField
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** On a TV the D-pad drives focus, so the on-screen keyboard must open on OK, not whenever focus lands on a field. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TvTextFieldTest {

    @get:Rule
    val compose = createComposeRule()

    private val focus = FocusRequester()

    private fun softInputVisible(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return shadowOf(context.getSystemService(InputMethodManager::class.java)).isSoftInputVisible
    }

    private fun showField() = compose.setContent {
        TilfazTheme { TvTextField(value = "", onValueChange = {}, label = "Server", modifier = Modifier.focusRequester(focus)) }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun sidewaysKeysMoveFocusOutOfAFieldThatIsNotBeingEdited() {
        compose.setContent {
            TilfazTheme {
                Row {
                    TvTextField(value = "", onValueChange = {}, label = "Search", modifier = Modifier.width(200.dp).focusRequester(focus))
                    Box(Modifier.size(80.dp).testTag("neighbour").focusable())
                }
            }
        }
        compose.runOnIdle { focus.requestFocus() }
        compose.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionRight) }

        compose.onNodeWithTag("neighbour").assertIsFocused()
    }

    @Test
    fun aBlankLabelTakesNoSpaceAboveTheBox() {
        compose.setContent {
            TilfazTheme { TvTextField(value = "", onValueChange = {}, label = "", modifier = Modifier.testTag("field")) }
        }

        val height = compose.onNodeWithTag("field").fetchSemanticsNode().boundsInRoot.height
        val box = with(compose.density) { 56.dp.toPx() }
        assertThat(height).isCloseTo(box, delta = 1f) // just the 56dp box, nothing above it
    }

    @Test
    fun focusingTheFieldDoesNotOpenTheKeyboard() {
        showField()
        compose.runOnIdle { focus.requestFocus() }
        compose.waitForIdle()

        assertThat(softInputVisible()).isFalse()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun pressingOkOnTheFocusedFieldOpensTheKeyboard() {
        showField()
        compose.runOnIdle { focus.requestFocus() }
        compose.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertThat(softInputVisible()).isTrue()
    }
}
