package com.androbito.tilfaz.feature.live

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.NowNext
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** On the smallest TV we support (960x540dp) all three columns must stay readable and inside the screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class LiveScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val longDescription = "A long programme synopsis that runs over many lines. ".repeat(30)

    private fun stateWith(favorite: Boolean = false): LiveUiState {
        val channel = Fixtures.alJazeera
        return LiveUiState(
            categories = listOf(LiveCategoryUi(LiveCategoryUi.ALL_ID, "", Fixtures.liveChannels.size, LiveCategoryUi.Kind.ALL)),
            channels = Fixtures.liveChannels,
            favoriteIds = if (favorite) setOf(channel.id) else emptySet(),
            nowNext = mapOf(
                channel.id to NowNext(
                    now = Fixtures.epg(channel.id, "News Hour", -30, 60).copy(description = longDescription),
                    next = Fixtures.epg(channel.id, "The Debate", 30, 60).copy(description = longDescription)
                )
            ),
            focusedChannelId = channel.id,
            isLoading = false
        )
    }

    private fun show(state: LiveUiState) = compose.setContent {
        TilfazTheme { LiveScreen(state = state, onEvent = {}, onPlay = {}) }
    }

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    @Test
    fun channelNamesHaveRoomInTheList() {
        show(stateWith())

        val row = compose.onAllNodesWithText(Fixtures.alJazeera.name, useUnmergedTree = true)[0].fetchSemanticsNode().boundsInRoot
        assertThat(row.width).isGreaterThanOrEqualTo(MIN_NAME_WIDTH_PX)
    }

    @Test
    fun previewPaneStaysInsideTheScreenEvenWithLongSynopses() {
        show(stateWith())

        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val hint = compose.onNodeWithText(string(R.string.live_press_ok), substring = true).fetchSemanticsNode().boundsInRoot
        // A child pushed past the bottom is laid out with zero size, so "inside the screen" needs a real height too.
        assertThat(hint.height).isGreaterThan(0f)
        assertThat(hint.bottom).isLessThanOrEqualTo(screen.bottom)
    }

    @Test
    fun aFavouritedChannelSaysSoInThePreview() {
        show(stateWith(favorite = true))

        compose.onNodeWithText(string(R.string.live_in_favorites)).assertIsDisplayed()
    }

    private companion object {
        const val MIN_NAME_WIDTH_PX = 200f // 100dp at 2px per dp: enough for a readable, ellipsized channel name
    }
}
