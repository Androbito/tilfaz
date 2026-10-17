package com.androbito.tilfaz.feature.home

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A remote has no pointer: Home must always have a focused tile, and Back must return to the one the user left. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val navigation = HomeNavigation({}, {}, {}, {}, {}, {})

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
}
