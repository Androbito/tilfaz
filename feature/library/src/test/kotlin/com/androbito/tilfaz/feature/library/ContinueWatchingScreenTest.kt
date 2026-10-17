package com.androbito.tilfaz.feature.library

import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
@OptIn(ExperimentalTestApi::class)
class ContinueWatchingScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<ContinueWatchingEvent>()
    private var playedMovie: Pair<Int, Long>? = null
    private var playedEpisode: Triple<Int, String, Long>? = null

    private val navigation = ContinueWatchingNavigation(
        onPlayMovie = { id, start -> playedMovie = id to start },
        onPlayEpisode = { seriesId, episodeId, start -> playedEpisode = Triple(seriesId, episodeId, start) }
    )

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun card(name: String) = hasText(name) and hasClickAction()

    private fun pressKey(matcher: SemanticsMatcher, key: Key) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private val movieProgress = WatchProgress("vod:501", ContentType.VOD, "m", null, 600_000, 1_200_000, Fixtures.now, "501")
    private val episodeProgress = WatchProgress("series:901:90101", ContentType.SERIES, "e", null, 300_000, 1_200_000, Fixtures.now, "901:90101")

    private val full = ContinueWatchingUiState(
        movies = listOf(ContinueMovie(Fixtures.movie, movieProgress)),
        shows = listOf(ContinueShow(Fixtures.series, episodeProgress, "90101"))
    )

    private fun show(state: ContinueWatchingUiState) = compose.setContent {
        TilfazTheme { ContinueWatchingScreen(state = state, onEvent = { events += it }, navigation = navigation) }
    }

    @Test
    fun movieAndShowShelvesAreAlwaysThereAndLiveIsNot() {
        show(ContinueWatchingUiState())

        compose.onNodeWithText(string(R.string.continue_movies)).assertExists()
        compose.onNodeWithText(string(R.string.continue_series)).assertExists()
        compose.onNodeWithText(string(R.string.continue_empty_movies)).assertExists()
        compose.onNodeWithText(string(R.string.continue_empty_series)).assertExists()
        compose.onNodeWithText("Live").assertDoesNotExist()
    }

    @Test
    fun cardsCarryTheirScore() {
        show(full)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(card(Fixtures.series.name))

        compose.onNodeWithText("7.0", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("6.0", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theFirstItemHasFocusWhenTheScreenOpens() {
        show(full)
        compose.waitForIdle()

        compose.onNode(card(Fixtures.movie.name)).assertIsFocused()
    }

    @Test
    fun okOnAMovieResumesItWhereItStopped() {
        show(full)

        pressKey(card(Fixtures.movie.name), Key.DirectionCenter)

        assertThat(playedMovie).isEqualTo(Fixtures.movie.id to 600_000L)
    }

    @Test
    fun okOnAShowResumesTheEpisodeWhereItStopped() {
        show(full)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(card(Fixtures.series.name))

        pressKey(card(Fixtures.series.name), Key.DirectionCenter)

        assertThat(playedEpisode).isEqualTo(Triple(Fixtures.series.id, "90101", 300_000L))
    }

    @Test
    fun theMenuKeyAsksBeforeMarkingAMovieAsWatched() {
        show(full)

        pressKey(card(Fixtures.movie.name), Key.Menu)

        assertThat(events).isEqualTo(listOf<ContinueWatchingEvent>(ContinueWatchingEvent.RequestMarkWatched(movieProgress, Fixtures.movie.name)))
    }

    @Test
    fun theQuestionOpensOnTheSafeChoiceAndBackCancelsIt() {
        var dispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            TilfazTheme {
                ContinueWatchingScreen(
                    state = full.copy(pendingWatched = PendingWatched(movieProgress, Fixtures.movie.name)),
                    onEvent = { events += it },
                    navigation = navigation
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(string(R.string.continue_mark_title)).assertExists()
        compose.onNode(card(string(R.string.continue_mark_cancel))).assertIsFocused()

        compose.runOnIdle { dispatcher!!.onBackPressed() }
        assertThat(events.last()).isEqualTo(ContinueWatchingEvent.CancelMarkWatched)
    }

    @Test
    fun confirmingMarksItAsWatched() {
        show(full.copy(pendingWatched = PendingWatched(movieProgress, Fixtures.movie.name)))
        compose.waitForIdle()

        pressKey(card(string(R.string.continue_mark_confirm)), Key.DirectionCenter)

        assertThat(events.last()).isEqualTo(ContinueWatchingEvent.ConfirmMarkWatched)
    }

    @Test
    fun afterMarkingFocusMovesToTheItemThatTakesItsPlace() {
        var state by mutableStateOf(full)
        compose.setContent {
            TilfazTheme {
                ContinueWatchingScreen(
                    state = state,
                    onEvent = { event ->
                        state = when (event) {
                            is ContinueWatchingEvent.RequestMarkWatched -> state.copy(pendingWatched = PendingWatched(event.progress, event.name))
                            ContinueWatchingEvent.ConfirmMarkWatched -> state.copy(movies = emptyList(), pendingWatched = null)
                            ContinueWatchingEvent.CancelMarkWatched -> state.copy(pendingWatched = null)
                        }
                    },
                    navigation = navigation
                )
            }
        }
        pressKey(card(Fixtures.movie.name), Key.Menu)

        pressKey(card(string(R.string.continue_mark_confirm)), Key.DirectionCenter)

        compose.onNode(card(Fixtures.series.name)).assertIsFocused()
    }

    @Test
    fun focusReturnsToTheItemTheUserLeftWhenTheScreenComesBack() {
        val tester = StateRestorationTester(compose)
        tester.setContent { TilfazTheme { ContinueWatchingScreen(state = full, onEvent = {}, navigation = navigation) } }
        compose.onNode(card(Fixtures.series.name)).requestFocus()
        compose.onNode(card(Fixtures.series.name)).assertIsFocused()

        tester.emulateSavedInstanceStateRestore()

        compose.onNode(card(Fixtures.series.name)).assertIsFocused()
    }
}
