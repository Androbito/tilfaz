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
import androidx.compose.ui.test.isFocused
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
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
@OptIn(ExperimentalTestApi::class)
class FavoritesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var events = mutableListOf<FavoritesEvent>()
    private var played: Int? = null

    private val navigation = FavoritesNavigation(onPlayChannel = { played = it }, onOpenMovie = {}, onOpenSeries = {})

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun card(name: String) = hasText(name) and hasClickAction()

    private fun pressKey(matcher: SemanticsMatcher, key: Key) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private val full = FavoritesUiState(
        channels = listOf(Fixtures.alJazeera),
        movies = listOf(Fixtures.movie),
        series = listOf(Fixtures.series)
    )

    private fun show(state: FavoritesUiState) = compose.setContent {
        TilfazTheme { FavoritesScreen(state = state, onEvent = { events += it }, navigation = navigation) }
    }

    @Test
    fun threeSectionsAreAlwaysThereWithAHintWhenEmpty() {
        show(FavoritesUiState())

        listOf(R.string.favorites_live, R.string.favorites_movies, R.string.favorites_series).forEach {
            compose.onNodeWithText(string(it)).assertExists()
        }
        listOf(R.string.favorites_empty_live, R.string.favorites_empty_movies, R.string.favorites_empty_series).forEach {
            compose.onNodeWithText(string(it)).assertExists()
        }
    }

    @Test
    fun moviesAndShowsCarryTheirScore() {
        show(full)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(card(Fixtures.series.name))

        compose.onNodeWithText("7.0", useUnmergedTree = true).assertExists() // the movie
        compose.onNodeWithText("6.0", useUnmergedTree = true).assertExists() // the show
    }

    @Test
    fun theFirstItemHasFocusWhenTheScreenOpens() {
        show(full)
        compose.waitForIdle()

        compose.onNode(card(Fixtures.alJazeera.name)).assertIsFocused()
    }

    @Test
    fun okOnAChannelPlaysIt() {
        show(full)

        pressKey(card(Fixtures.alJazeera.name), Key.DirectionCenter)

        assertThat(played).isEqualTo(Fixtures.alJazeera.id)
    }

    @Test
    fun theMenuKeyOnAMovieAsksBeforeRemovingIt() {
        show(full)

        pressKey(card(Fixtures.movie.name), Key.Menu)

        assertThat(events).isEqualTo(listOf<FavoritesEvent>(FavoritesEvent.RequestRemoval(ContentType.VOD, Fixtures.movie.id, Fixtures.movie.name)))
    }

    @Test
    fun theQuestionOpensOnTheSafeChoiceAndBackCancelsIt() {
        var dispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            TilfazTheme {
                FavoritesScreen(
                    state = full.copy(pendingRemoval = PendingRemoval(ContentType.VOD, Fixtures.movie.id, Fixtures.movie.name)),
                    onEvent = { events += it },
                    navigation = navigation
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(string(R.string.favorites_remove_title)).assertExists()
        compose.onNode(card(string(R.string.favorites_remove_cancel))).assertIsFocused()

        compose.runOnIdle { dispatcher!!.onBackPressed() }
        assertThat(events.last()).isEqualTo(FavoritesEvent.CancelRemoval)
    }

    @Test
    fun confirmingTheQuestionRemovesTheFavourite() {
        show(full.copy(pendingRemoval = PendingRemoval(ContentType.VOD, Fixtures.movie.id, Fixtures.movie.name)))
        compose.waitForIdle()

        pressKey(card(string(R.string.favorites_remove_confirm)), Key.DirectionCenter)

        assertThat(events.last()).isEqualTo(FavoritesEvent.ConfirmRemoval)
    }

    @Test
    fun afterARemovalFocusMovesToTheItemThatTakesItsPlace() {
        var state by mutableStateOf(full)
        compose.setContent {
            TilfazTheme {
                FavoritesScreen(
                    state = state,
                    onEvent = { event ->
                        state = when (event) {
                            is FavoritesEvent.RequestRemoval -> state.copy(pendingRemoval = PendingRemoval(event.type, event.id, event.name))
                            FavoritesEvent.ConfirmRemoval -> state.copy(movies = emptyList(), pendingRemoval = null)
                            FavoritesEvent.CancelRemoval -> state.copy(pendingRemoval = null)
                        }
                    },
                    navigation = navigation
                )
            }
        }
        pressKey(card(Fixtures.movie.name), Key.Menu)

        pressKey(card(string(R.string.favorites_remove_confirm)), Key.DirectionCenter)

        compose.onNode(card(Fixtures.series.name)).assertIsFocused()
    }

    @Test
    fun focusReturnsToTheItemTheUserLeftWhenTheScreenComesBack() {
        val tester = StateRestorationTester(compose)
        tester.setContent { TilfazTheme { FavoritesScreen(state = full, onEvent = {}, navigation = navigation) } }
        compose.onNode(card(Fixtures.movie.name)).requestFocus()
        compose.onNode(card(Fixtures.movie.name)).assertIsFocused()

        tester.emulateSavedInstanceStateRestore()

        compose.onNode(card(Fixtures.movie.name)).assertIsFocused()
    }

    @Test
    fun focusReturnsToAnItemThatIsFarAlongItsShelf() {
        val movies = (1..14).map { Fixtures.movie.copy(id = 700 + it, name = "Movie $it") }
        val tester = StateRestorationTester(compose)
        tester.setContent { TilfazTheme { FavoritesScreen(state = full.copy(movies = movies), onEvent = {}, navigation = navigation) } }
        compose.onNode(card("Movie 1")).requestFocus()
        repeat(12) { compose.onNode(hasClickAction() and isFocused()).performKeyInput { pressKey(Key.DirectionRight) } }
        compose.waitForIdle()
        compose.onNode(card("Movie 13")).assertIsFocused()

        tester.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        compose.onNode(card("Movie 13")).assertIsFocused()
    }

    @Test
    fun focusReturnsToAnItemInTheLowestShelf() {
        val tester = StateRestorationTester(compose)
        tester.setContent { TilfazTheme { FavoritesScreen(state = full, onEvent = {}, navigation = navigation) } }
        compose.onNode(card(Fixtures.alJazeera.name)).requestFocus()
        repeat(2) { compose.onNode(hasClickAction() and isFocused()).performKeyInput { pressKey(Key.DirectionDown) } }
        compose.waitForIdle()
        compose.onNode(card(Fixtures.series.name)).assertIsFocused()

        tester.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        compose.onNode(card(Fixtures.series.name)).assertIsFocused()
    }

    @Test
    fun focusFallsBackToTheNeighbourWhenTheItemTheUserLeftIsGone() {
        val movies = (1..3).map { Fixtures.movie.copy(id = 800 + it, name = "Movie $it") }
        var state by mutableStateOf(FavoritesUiState(movies = movies))
        val tester = StateRestorationTester(compose)
        tester.setContent { TilfazTheme { FavoritesScreen(state = state, onEvent = {}, navigation = navigation) } }
        compose.onNode(card("Movie 3")).requestFocus()
        compose.onNode(card("Movie 3")).assertIsFocused()

        // The user took it off the list on its page, so it is not there when they come back.
        state = FavoritesUiState(movies = movies.take(2))
        tester.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        compose.onNode(card("Movie 2")).assertIsFocused()
    }
}
