package com.androbito.tilfaz.feature.search

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androbito.tilfaz.core.designsystem.R as DesignR
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class SearchScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val navigation = SearchNavigation(onPlayChannel = {}, onOpenMovie = {}, onOpenSeries = {})

    private fun string(id: Int, vararg args: Any) = ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun showResults(channelCount: Int) {
        val channels = List(channelCount) { Fixtures.alJazeera.copy(id = it + 1, name = "Channel ${it + 1}") }
        compose.setContent {
            TilfazTheme { SearchScreen(SearchUiState(query = "channel", channels = channels), onQueryChanged = {}, navigation = navigation) }
        }
    }

    @Test
    fun channelResultsDoNotClaimThereIsNoProgrammeInfo() {
        showResults(channelCount = 3)

        compose.onAllNodesWithText(string(DesignR.string.ds_no_program), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun theHeaderSaysHowManyOfTheMatchesAreListed() {
        showResults(channelCount = 40)

        compose.onNodeWithText(string(R.string.search_section_truncated, string(R.string.search_channels), 30, 40)).assertIsDisplayed()
    }

    @Test
    fun theHeaderIsPlainWhenEverythingIsListed() {
        showResults(channelCount = 3)

        compose.onNodeWithText(string(R.string.search_section, string(R.string.search_channels), 3)).assertIsDisplayed()
    }
}
