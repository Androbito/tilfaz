package com.androbito.tilfaz.core.designsystem

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.component.onOptionKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class OptionKeyTest {

    @get:Rule
    val compose = createComposeRule()

    private var options = 0

    private fun showFocused() {
        compose.setContent { Box(Modifier.size(80.dp).testTag("item").onOptionKey { options++ }.focusable()) }
        compose.onNodeWithTag("item").requestFocus()
    }

    @Test
    fun theMenuKeyOnAFocusedItemOpensItsOptions() {
        showFocused()

        compose.onNodeWithTag("item").performKeyInput { pressKey(Key.Menu) }

        assertThat(options).isEqualTo(1)
    }

    @Test
    fun otherKeysAreLeftAlone() {
        showFocused()

        compose.onNodeWithTag("item").performKeyInput { pressKey(Key.DirectionCenter) }

        assertThat(options).isEqualTo(0)
    }
}
