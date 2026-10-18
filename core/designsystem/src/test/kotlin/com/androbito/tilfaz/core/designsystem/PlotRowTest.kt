package com.androbito.tilfaz.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androbito.tilfaz.core.designsystem.component.PlotRow
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Robolectric does not wrap text into lines, so what is shown is read from the row's expand/collapse state. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
@OptIn(ExperimentalTestApi::class)
class PlotRowTest {

    @get:Rule
    val compose = createComposeRule()

    private val folded = SemanticsMatcher.keyIsDefined(SemanticsActions.Expand)
    private val expanded = SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse)

    private fun show(text: String? = "A long synopsis") = compose.setContent {
        TilfazTheme { Column { PlotRow(label = "Plot", text = text, collapsedLines = 2) } }
    }

    private fun pressOnRow(key: Key) {
        compose.onNode(hasClickAction()).requestFocus()
        compose.onNode(hasClickAction()).performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    @Test
    fun theSynopsisCanTakeFocusAndStartsFolded() {
        show()

        compose.onNode(hasClickAction()).requestFocus()

        compose.onNode(hasClickAction()).assertIsFocused().assert(folded)
    }

    @Test
    fun okExpandsTheSynopsisAndOkAgainFoldsItBack() {
        show()

        pressOnRow(Key.DirectionCenter)
        compose.onNode(hasClickAction()).assert(expanded)

        pressOnRow(Key.DirectionCenter)
        compose.onNode(hasClickAction()).assert(folded)
    }

    @Test
    fun backFoldsAnExpandedSynopsisOnTheFirstPress() {
        show()
        pressOnRow(Key.DirectionCenter)

        pressOnRow(Key.Back)

        compose.onNode(hasClickAction()).assert(folded)
    }

    @Test
    fun withoutASynopsisNothingIsShown() {
        show(text = " ")

        compose.onNode(hasClickAction()).assertDoesNotExist()
    }
}
