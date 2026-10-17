package com.androbito.tilfaz.feature.series

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.domain.usecase.CatalogBrowser
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class SeriesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val breaking = Fixtures.series.copy(id = 11, name = "|EN| Breaking Point (2019)")

    private fun snapshot(query: String, suggestions: List<Series>) = CatalogBrowser.Snapshot(
        sections = listOf(CatalogBrowser.Section(CatalogBrowser.ALL, "", 1, CatalogBrowser.Section.Kind.ALL)),
        selectedSectionId = CatalogBrowser.ALL,
        favoriteIds = emptySet(),
        progressById = emptyMap(),
        query = query,
        suggestions = suggestions
    )

    @OptIn(ExperimentalTestApi::class)
    private fun pressOk(matcher: SemanticsMatcher) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    @Test
    fun pickingASuggestionOpensThatShowWithoutTheProvidersTag() {
        val opened = mutableListOf<Int>()
        compose.setContent {
            TilfazTheme {
                SeriesScreen(
                    SeriesUiState(snapshot("brea", listOf(breaking))),
                    items = flowOf(PagingData.from(listOf(breaking))).collectAsLazyPagingItems(),
                    onEvent = {},
                    onOpenSeries = { opened += it }
                )
            }
        }

        pressOk(hasText("Breaking Point (2019)") and hasClickAction())

        assertThat(opened).isEqualTo(listOf(11))
    }
}
