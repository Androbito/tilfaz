package com.androbito.tilfaz.feature.home

import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.shadows.ShadowToast

/** A remote has no pointer: Home must always have a focused tile, and Back must return to the one the user left. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var watchlistOpened = false

    private var exits = 0

    private val navigation = HomeNavigation({}, {}, {}, {}, {}, {}, onOpenWatchlist = { watchlistOpened = true }, onExit = { exits++ })

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    @Test
    fun theLiveTileHasFocusWhenHomeOpens() {
        compose.setContent { TilfazTheme { HomeScreen(HomeUiState(), onEvent = {}, navigation = navigation) } }
        compose.waitForIdle()

        compose.onNode(hasContentDescription(string(R.string.home_live_tv))).assertIsFocused()
    }

    @Test
    fun focusReturnsToTheTileTheUserLeftWhenHomeComesBack() {
        val tester = StateRestorationTester(compose)
        tester.setContent { TilfazTheme { HomeScreen(HomeUiState(), onEvent = {}, navigation = navigation) } }
        val movies = hasContentDescription(string(R.string.home_movies))
        compose.onNode(movies).requestFocus()
        compose.onNode(movies).assertIsFocused()

        tester.emulateSavedInstanceStateRestore()

        compose.onNode(movies).assertIsFocused()
    }

    @Test
    fun thereIsNoSearchButtonOnHome() {
        compose.setContent { TilfazTheme { HomeScreen(state = HomeUiState(), onEvent = {}, navigation = navigation) } }

        compose.onAllNodes(hasContentDescription("Search")).assertCountEquals(0)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun theBottomRowHasAWatchListTileInsteadOfSettings() {
        compose.setContent { TilfazTheme { HomeScreen(state = HomeUiState(), onEvent = {}, navigation = navigation) } }
        val watchlist = hasText(string(R.string.home_watchlist)) and hasClickAction()

        compose.onNode(watchlist).requestFocus()
        compose.onNode(watchlist).performKeyInput { pressKey(Key.DirectionCenter) }

        assertThat(watchlistOpened).isEqualTo(true)
        compose.onAllNodesWithText(string(R.string.home_settings)).assertCountEquals(0) // Settings stays in the top bar
        compose.onAllNodes(hasContentDescription(string(R.string.home_settings))).assertCountEquals(1)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun upFromTheBottomRowLandsOnTheBigTileAboveNotOnItsRefreshButton() {
        compose.setContent { TilfazTheme { HomeScreen(state = HomeUiState(), onEvent = {}, navigation = navigation) } }

        listOf(
            R.string.home_favorites to R.string.home_live_tv,
            R.string.home_continue to R.string.home_movies,
            R.string.home_watchlist to R.string.home_series
        ).forEach { (pill, tile) ->
            val pillNode = hasText(string(pill)) and hasClickAction()
            compose.onNode(pillNode).requestFocus()
            compose.onNode(pillNode).performKeyInput { pressKey(Key.DirectionUp) }
            compose.waitForIdle()

            compose.onNode(hasContentDescription(string(tile))).assertIsFocused()
        }
    }

    private fun showWithDispatcher(): () -> Unit {
        var dispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            TilfazTheme { HomeScreen(state = HomeUiState(), onEvent = {}, navigation = navigation) }
        }
        compose.waitForIdle()
        return { compose.runOnIdle { dispatcher!!.onBackPressed() } }
    }

    @Test
    fun theFirstBackOnHomeOnlyAsksAndTheSecondOneLeavesTheApp() {
        val back = showWithDispatcher()

        back()
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(string(R.string.home_press_back_again))
        assertThat(exits).isEqualTo(0)

        back()
        assertThat(exits).isEqualTo(1)
    }

    @Test
    fun aSecondBackAfterTheWindowAsksAgainInsteadOfLeaving() {
        val back = showWithDispatcher()
        back()

        ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
        back()

        assertThat(exits).isEqualTo(0)
        assertThat(ShadowToast.shownToastCount()).isEqualTo(2)
    }
}
