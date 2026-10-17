package com.androbito.tilfaz.feature.vod

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.domain.usecase.CatalogBrowser
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class MoviesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val scream = Fixtures.movie.copy(id = 7, name = "|FR| Scream 4 (2011)")
    private val screamers = Fixtures.movie.copy(id = 8, name = "|EN| Screamers")

    private fun snapshot(query: String, suggestions: List<com.androbito.tilfaz.core.model.Movie>) = CatalogBrowser.Snapshot(
        sections = listOf(CatalogBrowser.Section(CatalogBrowser.ALL, "", 2, CatalogBrowser.Section.Kind.ALL)),
        selectedSectionId = CatalogBrowser.ALL,
        items = listOf(scream, screamers),
        favoriteIds = emptySet(),
        progressById = emptyMap(),
        query = query,
        suggestions = suggestions
    )

    private fun show(query: String, opened: MutableList<Int> = mutableListOf()) = compose.setContent {
        TilfazTheme {
            MoviesScreen(
                state = MoviesUiState(snapshot(query, if (query.isBlank()) emptyList() else listOf(scream, screamers))),
                onEvent = {},
                onOpenMovie = { opened += it }
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun pressOk(matcher: SemanticsMatcher) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    @Test
    fun suggestionsShowTheTitleWithoutTheProvidersTag() {
        show("scr")

        compose.onNode(hasText("Scream 4 (2011)") and hasClickAction()).assertExists()
        compose.onNode(hasText("Screamers") and hasClickAction()).assertExists()
    }

    @Test
    fun pickingASuggestionOpensThatMovie() {
        val opened = mutableListOf<Int>()
        show("scr", opened)

        pressOk(hasText("Scream 4 (2011)") and hasClickAction())

        assertThat(opened).isEqualTo(listOf(7))
    }

    @Test
    fun withoutAQueryThereAreNoSuggestionRows() {
        show("")

        compose.onNodeWithText("Scream 4 (2011)").assertDoesNotExist()
    }
}
