package com.androbito.tilfaz.feature.series

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class SeriesDetailsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private fun show() = compose.setContent {
        TilfazTheme {
            SeriesDetailsScreen(
                state = SeriesDetailsUiState(series = Fixtures.series, details = Fixtures.seriesDetails, isLoading = false, selectedSeason = 1),
                onEvent = {},
                onPlay = {},
                onOpenSeries = {}
            )
        }
    }

    @Test
    fun theMainPlayButtonHasFocusWhenThePageOpens() {
        show()
        compose.waitForIdle()

        compose.onNode(hasText("Play S01E01") and hasClickAction()).assertIsFocused()
    }

    @Test
    fun focusReturnsToTheEpisodeTheUserPlayedWhenThePageComesBack() {
        val tester = StateRestorationTester(compose)
        tester.setContent {
            TilfazTheme {
                SeriesDetailsScreen(
                    state = SeriesDetailsUiState(series = Fixtures.series, details = Fixtures.seriesDetails, isLoading = false, selectedSeason = 1),
                    onEvent = {},
                    onPlay = {},
                    onOpenSeries = {}
                )
            }
        }
        val secondEpisode = hasText("S01E02") and hasClickAction()
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(secondEpisode)
        compose.onNode(secondEpisode).requestFocus()
        compose.onNode(secondEpisode).assertIsFocused()

        // Playing an episode disposes this page; Back recreates it from saved state.
        tester.emulateSavedInstanceStateRestore()

        compose.onNode(secondEpisode).assertIsFocused()
    }

    @Test
    fun theScoreAndMoreLikeThisAppearLikeOnAMoviePage() {
        val other = Fixtures.series.copy(id = 902, name = "A related show")
        compose.setContent {
            TilfazTheme {
                SeriesDetailsScreen(
                    state = SeriesDetailsUiState(
                        series = Fixtures.series,
                        details = Fixtures.seriesDetails,
                        isLoading = false,
                        selectedSeason = 1,
                        related = listOf(other)
                    ),
                    onEvent = {},
                    onPlay = {},
                    onOpenSeries = {}
                )
            }
        }

        compose.onNodeWithText("6.0 / 10").assertExists()
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("A related show"))
        compose.onNodeWithText(context.getString(R.string.series_related)).assertExists()
    }

    @Test
    fun anEpisodeWhoseTitleIsItsCodeShowsTheCodeOnce() {
        show()

        compose.onAllNodesWithText("S01E01  S01E01", useUnmergedTree = true).assertCountEquals(0)
    }
}
