package com.androbito.tilfaz.core.designsystem

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.androbito.tilfaz.core.designsystem.component.BrowserLayout
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class BrowserLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val browser: @Composable () -> Unit = {
        TilfazTheme {
            BrowserLayout(
                query = "",
                onQueryChange = {},
                searchPlaceholder = "Search",
                sections = {},
                items = (1..30).toList(),
                itemKey = { it },
                emptyContent = {}
            ) { number -> Box(Modifier.size(100.dp).testTag("item$number").focusable()) }
        }
    }

    @Test
    fun postersAreShownFourPerRow() {
        compose.setContent(browser)
        compose.waitForIdle()

        fun top(number: Int) = compose.onNodeWithTag("item$number").fetchSemanticsNode().boundsInRoot.top
        assertThat(top(4)).isEqualTo(top(1))
        assertThat(top(5)).isGreaterThan(top(1))
    }

    @Test
    fun theFirstItemHasFocusWhenTheScreenOpens() {
        compose.setContent(browser)
        compose.waitForIdle()

        compose.onNodeWithTag("item1").assertIsFocused()
    }

    @Test
    fun focusReturnsToTheItemTheUserLeftWhenTheScreenComesBack() {
        val tester = StateRestorationTester(compose)
        tester.setContent(browser)
        compose.onNodeWithTag("item5").requestFocus()
        compose.onNodeWithTag("item5").assertIsFocused()

        // Opening a detail page disposes this screen; coming back recreates it from saved state.
        tester.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("item5").assertIsFocused()
    }

    private fun withSuggestions(query: String): @Composable () -> Unit = {
        TilfazTheme {
            BrowserLayout(
                query = query,
                onQueryChange = {},
                searchPlaceholder = "Search",
                sections = { item { Box(Modifier.size(40.dp).testTag("sections").focusable()) } },
                suggestions = { item { Box(Modifier.size(40.dp).testTag("suggestions").focusable()) } },
                items = (1..3).toList(),
                itemKey = { it },
                emptyContent = {}
            ) { number -> Box(Modifier.size(100.dp).testTag("item$number").focusable()) }
        }
    }

    @Test
    fun whileTypingTheSuggestionsTakeTheSectionList() {
        compose.setContent(withSuggestions("scr"))
        compose.waitForIdle()

        compose.onNodeWithTag("suggestions").assertExists()
        compose.onNodeWithTag("sections").assertDoesNotExist()
    }

    @Test
    fun withoutAQueryTheSectionsAreShown() {
        compose.setContent(withSuggestions(""))
        compose.waitForIdle()

        compose.onNodeWithTag("sections").assertExists()
        compose.onNodeWithTag("suggestions").assertDoesNotExist()
    }

    @Test
    fun aBlankQueryCountsAsNoQuery() {
        compose.setContent(withSuggestions("   "))
        compose.waitForIdle()

        compose.onNodeWithTag("sections").assertExists()
    }
}
