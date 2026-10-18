package com.androbito.tilfaz.feature.library

import android.content.Context
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
@OptIn(ExperimentalTestApi::class)
class WatchlistScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var events = mutableListOf<WatchlistEvent>()
    private var openedMovie: Int? = null
    private var openedSeries: Int? = null

    private val navigation = WatchlistNavigation(onOpenMovie = { openedMovie = it }, onOpenSeries = { openedSeries = it })

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun card(name: String) = hasText(name) and hasClickAction()

    private fun pressKey(matcher: SemanticsMatcher, key: Key) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private val full = WatchlistUiState(movies = listOf(Fixtures.movie), series = listOf(Fixtures.series))

    private fun show(state: WatchlistUiState) = compose.setContent {
        TilfazTheme { WatchlistScreen(state = state, onEvent = { events += it }, navigation = navigation) }
    }

    @Test
    fun bothSectionsAreAlwaysThereWithAHintWhenEmpty() {
        show(WatchlistUiState())

        listOf(R.string.watchlist_movies, R.string.watchlist_series, R.string.watchlist_empty_movies, R.string.watchlist_empty_series).forEach {
            compose.onNodeWithText(string(it)).assertExists()
        }
    }

    @Test
    fun theFirstItemHasFocusWhenTheScreenOpens() {
        show(full)
        compose.waitForIdle()

        compose.onNode(card(Fixtures.movie.name)).assertIsFocused()
    }

    @Test
    fun okOnAMovieOpensItsPage() {
        show(full)

        pressKey(card(Fixtures.movie.name), Key.DirectionCenter)

        assertThat(openedMovie).isEqualTo(Fixtures.movie.id)
    }

    @Test
    fun okOnAShowOpensItsPage() {
        show(full)

        pressKey(card(Fixtures.series.name), Key.DirectionCenter)

        assertThat(openedSeries).isEqualTo(Fixtures.series.id)
    }

    @Test
    fun theMenuKeyAsksBeforeTakingAnItemOff() {
        show(full)

        pressKey(card(Fixtures.movie.name), Key.Menu)

        assertThat(events).isEqualTo(listOf<WatchlistEvent>(WatchlistEvent.RequestRemoval(ContentType.VOD, Fixtures.movie.id, Fixtures.movie.name)))
    }

    @Test
    fun confirmingTheQuestionTakesTheItemOff() {
        show(full.copy(pendingRemoval = PendingRemoval(ContentType.VOD, Fixtures.movie.id, Fixtures.movie.name)))
        compose.waitForIdle()

        pressKey(card(string(R.string.watchlist_remove_confirm)), Key.DirectionCenter)

        assertThat(events.last()).isEqualTo(WatchlistEvent.ConfirmRemoval)
    }
}
