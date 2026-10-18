package com.androbito.tilfaz.feature.vod

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.component.backKeyForDispatcherOnly
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
@OptIn(ExperimentalTestApi::class)
class MovieDetailsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(sdk = [32])
    fun theBackKeyOnAFocusedButtonGoesBackOnTheFirstPress() {
        var backs = 0
        compose.setContent {
            BackHandler { backs++ }
            TilfazTheme {
                Box(Modifier.backKeyForDispatcherOnly()) {
                    MovieDetailsScreen(state = MovieDetailsUiState(movie = Fixtures.movie), onEvent = {}, onPlay = {}, onOpenMovie = {})
                }
            }
        }
        val play = hasText("Play") and hasClickAction()
        compose.onNode(play).requestFocus()

        compose.onNode(play).performKeyInput { pressKey(Key.Back) }

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun theSynopsisTakesFocusAndOkOpensItInFull() {
        val details = MovieDetails(Fixtures.movie, "A synopsis", null, null, null, null, null, emptyList(), null)
        compose.setContent {
            TilfazTheme {
                MovieDetailsScreen(state = MovieDetailsUiState(movie = Fixtures.movie, details = details), onEvent = {}, onPlay = {}, onOpenMovie = {})
            }
        }
        val plot = hasText("A synopsis") and hasClickAction()
        compose.onNode(plot).requestFocus()
        compose.onNode(plot).assertIsFocused().assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))

        compose.onNode(plot).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        compose.onNode(plot).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
    }

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun show(state: MovieDetailsUiState, onEvent: (MovieDetailsEvent) -> Unit) = compose.setContent {
        TilfazTheme { MovieDetailsScreen(state = state, onEvent = onEvent, onPlay = {}, onOpenMovie = {}) }
    }

    @Test
    fun theWatchListButtonAsksToToggle() {
        var event: MovieDetailsEvent? = null
        show(MovieDetailsUiState(movie = Fixtures.movie)) { event = it }
        val button = hasContentDescription(string(R.string.vod_add_watchlist)) and hasClickAction()
        compose.onNode(button).requestFocus()

        compose.onNode(button).performKeyInput { pressKey(Key.DirectionCenter) }

        assertThat(event).isEqualTo(MovieDetailsEvent.ToggleWatchlist)
    }

    @Test
    fun aMovieOnTheWatchListOffersToTakeItOff() {
        show(MovieDetailsUiState(movie = Fixtures.movie, isInWatchlist = true)) {}

        compose.onNode(hasContentDescription(string(R.string.vod_remove_watchlist)) and hasClickAction()).assertExists()
    }

    @Test
    fun startOverFavouriteAndWatchListAreIconsWithAnAccessibleName() {
        val resuming = WatchProgress("vod:${Fixtures.movie.id}", ContentType.VOD, "x", null, 900_000, 6_000_000, Fixtures.now, "${Fixtures.movie.id}")
        show(MovieDetailsUiState(movie = Fixtures.movie, progress = resuming)) {}

        listOf(R.string.vod_start_over, R.string.vod_add_favorite, R.string.vod_add_watchlist).forEach {
            compose.onAllNodesWithText(string(it)).assertCountEquals(0) // no visible label
            compose.onNode(hasContentDescription(string(it)) and hasClickAction()).assertExists()
        }
    }

    @Test
    fun focusingTheFirstButtonAgainScrollsBackToTheTopOfThePage() {
        val long = "A long synopsis. ".repeat(300)
        val details = MovieDetails(Fixtures.movie, long, "Cast ".repeat(60), "Director", "Drama", "2026", 6000, emptyList(), null)
        compose.setContent {
            TilfazTheme {
                // A short window, so that the synopsis lies below it and reaching it scrolls the page.
                Box(Modifier.height(320.dp)) {
                    MovieDetailsScreen(state = MovieDetailsUiState(movie = Fixtures.movie, details = details), onEvent = {}, onPlay = {}, onOpenMovie = {})
                }
            }
        }
        compose.waitForIdle()
        val title = compose.onNodeWithText(Fixtures.movie.name)
        val topAtStart = title.fetchSemanticsNode().boundsInRoot.top

        // Down to the synopsis, which scrolls the page; then back up to the Play button.
        val play = hasText("Play") and hasClickAction()
        val plot = hasText("A long synopsis.", substring = true) and hasClickAction()
        compose.onNode(play).requestFocus()
        compose.onNode(play).performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNode(plot).assertIsFocused()
        assertThat(title.fetchSemanticsNode().boundsInRoot.top < topAtStart).isEqualTo(true) // the page really scrolled
        compose.onNode(plot).performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()

        assertThat(title.fetchSemanticsNode().boundsInRoot.top).isEqualTo(topAtStart)
    }
}
