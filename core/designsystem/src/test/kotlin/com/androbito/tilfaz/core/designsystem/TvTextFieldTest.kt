package com.androbito.tilfaz.core.designsystem

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
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
