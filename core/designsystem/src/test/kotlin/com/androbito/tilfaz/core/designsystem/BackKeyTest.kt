package com.androbito.tilfaz.core.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.component.backKeyForDispatcherOnly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Back must reach the back dispatcher (BackHandler, Navigation) and nothing else. Left alone, Compose also reads Back as
 * "move focus out of the focused item", which inside a focus group succeeds, drops focus and eats the press.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class BackKeyTest {

    @get:Rule
    val compose = createComposeRule()

    private var backs = 0

    /** The item sits inside something focusable, which is where Compose's "move focus out" sends focus. */
    private fun show(item: Modifier = Modifier) {
        compose.setContent {
            BackHandler { backs++ }
            Box(Modifier.backKeyForDispatcherOnly().focusable()) {
                Box(Modifier.size(80.dp).testTag("item").then(item).focusable())
            }
        }
        compose.onNodeWithTag("item").requestFocus()
    }

    @Test
    @Config(sdk = [34])
    fun `from Android 13 the system calls the dispatcher, so the key only has to leave focus alone`() {
        show()

        compose.onNodeWithTag("item").performKeyInput { pressKey(Key.Back) }

        compose.onNodeWithTag("item").assertIsFocused()
        assertThat(backs).isEqualTo(0)
    }

    @Test
    @Config(sdk = [32])
    fun `before Android 13 the key is the back press, once, and focus stays`() {
        show()

        compose.onNodeWithTag("item").performKeyInput { pressKey(Key.Back) }

        compose.onNodeWithTag("item").assertIsFocused()
        assertThat(backs).isEqualTo(1)
    }

    @Test
    @Config(sdk = [32])
    fun `a key that is still down does not go back yet`() {
        show()

        compose.onNodeWithTag("item").performKeyInput { keyDown(Key.Back) }

        assertThat(backs).isEqualTo(0)
    }

    @Test
    @Config(sdk = [32])
    fun `an element that wants Back keeps it`() {
        show(Modifier.onKeyEvent { it.key == Key.Back && it.type != KeyEventType.Unknown })

        compose.onNodeWithTag("item").performKeyInput { pressKey(Key.Back) }

        assertThat(backs).isEqualTo(0)
    }

    @Test
    @Config(sdk = [32])
    fun `other keys are left alone`() {
        show()

        compose.onNodeWithTag("item").performKeyInput { pressKey(Key.DirectionCenter) }

        assertThat(backs).isEqualTo(0)
    }
}
