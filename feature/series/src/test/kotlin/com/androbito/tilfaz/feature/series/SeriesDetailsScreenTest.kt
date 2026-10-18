package com.androbito.tilfaz.feature.series

import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.Season
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.WatchProgress
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

    private fun string(id: Int, vararg args: Any) = ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val episodesButton get() = hasText(string(R.string.series_show_episodes)) and hasClickAction()

    private fun episode(code: String) = hasText(code) and hasClickAction()

    /** A remote's OK key on a focused node: TV Material surfaces are driven by keys, not by touch. */
    @OptIn(ExperimentalTestApi::class)
    private fun pressOk(matcher: SemanticsMatcher) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private fun finished(episodeId: String) = WatchProgress(
        playableKey = "series:901:$episodeId",
        contentType = ContentType.SERIES,
        title = "x",
        posterUrl = null,
        positionMs = 2_500_000,
        durationMs = 2_580_000,
        updatedAt = Fixtures.now,
        reference = "901:$episodeId"
    )

    private val twoSeasons = Fixtures.seriesDetails.copy(
        seasons = Fixtures.seriesDetails.seasons + Season(
            2,
            "Season 2",
            listOf(Fixtures.episode1.copy(id = "90201", seasonNumber = 2, episodeNumber = 1, title = "S02E01"))
        )
    )

    private fun state(
        details: SeriesDetails = Fixtures.seriesDetails,
        progress: Map<String, WatchProgress> = emptyMap(),
        related: List<Series> = emptyList()
    ) = SeriesDetailsUiState(
        series = Fixtures.series,
        details = details,
        isLoading = false,
        selectedSeason = 1,
        progressByEpisodeId = progress,
        related = related
    )

    private fun show(state: SeriesDetailsUiState = state(), onEvent: (SeriesDetailsEvent) -> Unit = {}) = compose.setContent {
        TilfazTheme { SeriesDetailsScreen(state = state, onEvent = onEvent, onPlay = {}, onOpenSeries = {}) }
    }

    @Test
    fun theMainPlayButtonHasFocusWhenThePageOpens() {
        show()
        compose.waitForIdle()

        compose.onNode(hasText("Play S01E01") and hasClickAction()).assertIsFocused()
    }

    @Test
    fun episodesAreNotOnThePageUntilTheButtonIsPressed() {
        show()

        compose.onAllNodesWithText("S01E02", useUnmergedTree = true).assertCountEquals(0)
        pressOk(episodesButton)

        compose.onAllNodesWithText("S01E02", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun pressingEpisodesFocusesTheEpisodeThatWouldPlayNext() {
        show(state(progress = mapOf("90101" to finished("90101"))))
        compose.waitForIdle()

        pressOk(episodesButton)

        compose.onNode(episode("S01E02")).assertIsFocused()
    }

    @Test
    fun theSeasonChipsAskForTheirSeason() {
        var event: SeriesDetailsEvent? = null
        show(state(details = twoSeasons), onEvent = { event = it })
        pressOk(episodesButton)

        pressOk(hasText(string(R.string.series_season, 2)) and hasClickAction())

        assertThat(event).isEqualTo(SeriesDetailsEvent.SelectSeason(2))
    }

    @Test
    fun backClosesThePanelAndGivesFocusBackToTheEpisodesButton() {
        var dispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            TilfazTheme { SeriesDetailsScreen(state = state(), onEvent = {}, onPlay = {}, onOpenSeries = {}) }
        }
        pressOk(episodesButton)

        compose.runOnIdle { dispatcher!!.onBackPressed() }
        compose.waitForIdle()

        compose.onAllNodesWithText("S01E02", useUnmergedTree = true).assertCountEquals(0)
        compose.onNode(episodesButton).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theBackKeyOnAFocusedButtonGoesBackOnTheFirstPress() {
        var backs = 0
        compose.setContent {
            BackHandler { backs++ }
            TilfazTheme { SeriesDetailsScreen(state = state(), onEvent = {}, onPlay = {}, onOpenSeries = {}) }
        }
        compose.waitForIdle()

        compose.onNode(hasText("Play S01E01") and hasClickAction()).performKeyInput { pressKey(Key.Back) }

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun focusReturnsToTheEpisodeTheUserPlayedWhenThePageComesBack() {
        val tester = StateRestorationTester(compose)
        tester.setContent {
            TilfazTheme { SeriesDetailsScreen(state = state(), onEvent = {}, onPlay = {}, onOpenSeries = {}) }
        }
        pressOk(episodesButton)
        compose.onNode(episode("S01E02")).requestFocus()
        compose.onNode(episode("S01E02")).assertIsFocused()

        // Playing an episode disposes this page; Back recreates it from saved state, panel still open.
        tester.emulateSavedInstanceStateRestore()

        compose.onNode(episode("S01E02")).assertIsFocused()
    }

    @Test
    fun finishedAndNextUpEpisodesAreMarked() {
        show(state(progress = mapOf("90101" to finished("90101"))))
        pressOk(episodesButton)

        compose.onNodeWithText(string(R.string.series_watched), useUnmergedTree = true).assertExists()
        compose.onNodeWithText(string(R.string.series_next_up), useUnmergedTree = true).assertExists()
    }

    @Test
    fun anEpisodeWhoseTitleIsItsCodeShowsTheCodeOnce() {
        show()
        pressOk(episodesButton)

        compose.onAllNodesWithText("S01E01  S01E01", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun theScoreAndMoreLikeThisSitInTheDetailsPage() {
        val other = Fixtures.series.copy(id = 902, name = "A related show")
        show(state(related = listOf(other)))

        compose.onNodeWithText("6.0 / 10").assertExists()
        compose.onNodeWithText(string(R.string.series_related)).assertExists()
    }
}
