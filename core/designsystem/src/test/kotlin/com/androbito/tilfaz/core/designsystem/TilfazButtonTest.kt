package com.androbito.tilfaz.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Text
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.component.TilfazButton
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The primary button reads as a button in every state: white label, and only a disabled one is dimmed. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TilfazButtonTest {

    @get:Rule
    val compose = createComposeRule()

    private fun labelColor(): Color {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Go", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.first().layoutInput.style.color
    }

    private fun show(enabled: Boolean = true) =
        compose.setContent { TilfazTheme { TilfazButton(onClick = {}, enabled = enabled) { Text("Go") } } }

    @Test
    fun labelIsWhiteWhenEnabled() {
        show()

        assertThat(labelColor()).isEqualTo(TilfazColors.OnAccent)
    }

    @Test
    fun labelStaysWhiteWhenFocused() {
        show()
        compose.onNode(hasClickAction()).requestFocus()
        compose.waitForIdle()

        assertThat(labelColor()).isEqualTo(TilfazColors.OnAccent)
    }

    @Test
    fun labelIsDimmedWhenDisabled() {
        show(enabled = false)

        assertThat(labelColor()).isEqualTo(TilfazColors.OnSurfaceMuted)
    }
}
