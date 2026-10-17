package com.androbito.tilfaz.feature.live

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.NowNext
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import kotlinx.coroutines.flow.flowOf
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
            favoriteIds = if (favorite) setOf(channel.id) else emptySet(),
            nowNext = mapOf(
                channel.id to NowNext(
                    now = Fixtures.epg(channel.id, "News Hour", -30, 60).copy(description = longDescription),
                    next = Fixtures.epg(channel.id, "The Debate", 30, 60).copy(description = longDescription)
                )
            ),
            focusedChannel = channel,
            isLoading = false
        )
    }

    private fun pages(channels: List<LiveChannel> = Fixtures.liveChannels) = flowOf(PagingData.from(channels))

    private fun show(state: LiveUiState) = compose.setContent {
        TilfazTheme { LiveScreen(state = state, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}) }
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
    fun theRightPaneStillFitsWithALongChannelNameLongProgrammeTitlesAndTheSmallVideo() {
        val long = Fixtures.alJazeera.copy(name = "|AR| BEIN SPORTS GLOBAL HD NX WITH A VERY LONG EXTRA NAME THAT WRAPS")
        val longText = "Diving - 1M Springboard Final - Asian Games with a very long title that needs more than one line"
        val state = stateWith().copy(
            focusedChannel = long,
            previewChannel = long,
            nowNext = mapOf(
                long.id to NowNext(
                    now = Fixtures.epg(long.id, longText, -30, 60).copy(description = longDescription),
                    next = Fixtures.epg(long.id, longText, 30, 60).copy(description = longDescription)
                )
            )
        )
        compose.setContent {
            TilfazTheme {
                LiveScreen(state = state, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}, preview = previewSlot(mutableListOf()))
            }
        }
        compose.waitForIdle()

        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val hint = compose.onNodeWithText(string(R.string.live_press_ok), substring = true).fetchSemanticsNode().boundsInRoot
        assertThat(hint.height).isGreaterThan(0f)
        assertThat(hint.bottom).isLessThanOrEqualTo(screen.bottom)
    }

    @Test
    fun comingBackFromFullScreenTheSmallVideoWaitsOutThePauseAgain() {
        val onScreen = mutableListOf<Int>()
        val state = stateWith().copy(previewChannel = Fixtures.alJazeera)
        val tester = StateRestorationTester(compose)
        compose.mainClock.autoAdvance = false
        tester.setContent {
            TilfazTheme {
                LiveScreen(state = state, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}, preview = previewSlot(onScreen))
            }
        }
        compose.mainClock.advanceTimeByFrame()
        pressOk(hasText(Fixtures.beinSports1.name) and hasClickAction()) // opens full screen: the small video goes away

        // The screen is rebuilt when Back brings it again; a stream started this instant would be cut a moment later.
        tester.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeByFrame()
        assertThat(onScreen).isEqualTo(emptyList())

        compose.mainClock.advanceTimeBy(LiveViewModel.PREVIEW_DELAY_MS + 100)
        assertThat(onScreen).isEqualTo(listOf(Fixtures.alJazeera.id))
    }

    @Test
    fun aFavouritedChannelSaysSoInThePreview() {
        show(stateWith(favorite = true))

        compose.onNodeWithText(string(R.string.live_in_favorites)).assertIsDisplayed()
    }

    @Test
    fun theFirstChannelHasFocusWhenTheScreenOpens() {
        show(stateWith())
        compose.waitForIdle()

        compose.onNode(hasText(Fixtures.alJazeera.name) and hasClickAction()).assertIsFocused()
    }

    @Test
    fun focusReturnsToTheChannelTheUserLeftWhenTheScreenComesBack() {
        val tester = StateRestorationTester(compose)
        tester.setContent { TilfazTheme { LiveScreen(state = stateWith(), channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}) } }
        val secondRow = hasText(Fixtures.beinSports1.name) and hasClickAction()
        compose.onNode(secondRow).requestFocus()
        compose.onNode(secondRow).assertIsFocused()

        // Playing a channel disposes this screen; Back recreates it from saved state.
        tester.emulateSavedInstanceStateRestore()

        compose.onNode(secondRow).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun pressOk(matcher: SemanticsMatcher) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    @Test
    fun whileTypingTheSuggestionsReplaceTheCategoriesAndPlayTheChannel() {
        val played = mutableListOf<Int>()
        val typing = stateWith().copy(query = "jaz", suggestions = listOf(Fixtures.alJazeera))
        compose.setContent {
            TilfazTheme { LiveScreen(state = typing, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = { played += it.id }) }
        }

        compose.onNodeWithText(string(R.string.live_all)).assertDoesNotExist()
        pressOk(hasText("AL JAZEERA HD") and hasClickAction())

        assertThat(played).isEqualTo(listOf(Fixtures.alJazeera.id))
    }

    @Test
    fun withoutAQueryTheCategoriesAreShown() {
        show(stateWith())

        compose.onNodeWithText(string(R.string.live_all)).assertIsDisplayed()
    }

    @Test
    fun aSearchedChannelZapsInItsOwnCategory() {
        val searching = stateWith().copy(query = "bein", selectedCategoryId = "2")

        assertThat(zapCategoryFor(searching, Fixtures.beinSports1)).isEqualTo("1")
    }

    @Test
    fun withoutASearchTheSelectedProviderCategoryIsUsed() {
        val browsing = stateWith().copy(
            categories = listOf(LiveCategoryUi("2", "|AR| NEWS", 1, LiveCategoryUi.Kind.PROVIDER)),
            selectedCategoryId = "2"
        )

        assertThat(zapCategoryFor(browsing, Fixtures.alJazeera)).isEqualTo("2")
        assertThat(zapCategoryFor(stateWith(), Fixtures.alJazeera)).isNull()
    }

    /** Stands in for the player feature's small video: focusable, clickable, and it records that it is on screen. */
    private fun previewSlot(onScreen: MutableList<Int>): @Composable (LiveChannel, Modifier, () -> Unit) -> Unit = { channel, modifier, onOpen ->
        DisposableEffect(channel.id) {
            onScreen += channel.id
            onDispose { onScreen -= channel.id }
        }
        Box(modifier.testTag("preview").clickable(onClick = onOpen))
    }

    @Test
    fun theSmallVideoShowsTheChannelThatIsBeingPreviewed() {
        val onScreen = mutableListOf<Int>()
        val state = stateWith().copy(previewChannel = Fixtures.alJazeera)
        compose.setContent {
            TilfazTheme { LiveScreen(state = state, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}, preview = previewSlot(onScreen)) }
        }
        compose.waitForIdle()

        assertThat(onScreen).isEqualTo(listOf(Fixtures.alJazeera.id))
    }

    @Test
    fun noVideoIsStartedWhileScrolling() {
        val onScreen = mutableListOf<Int>()
        val state = stateWith().copy(previewChannel = null)
        compose.setContent {
            TilfazTheme { LiveScreen(state = state, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}, preview = previewSlot(onScreen)) }
        }
        compose.waitForIdle()

        assertThat(onScreen).isEqualTo(emptyList())
        compose.onNodeWithTag("preview").assertDoesNotExist()
    }

    @Test
    fun okOnTheSmallVideoOpensThatChannelFullScreen() {
        val played = mutableListOf<Int>()
        val state = stateWith().copy(previewChannel = Fixtures.alJazeera)
        compose.setContent {
            TilfazTheme {
                LiveScreen(state = state, channels = pages().collectAsLazyPagingItems(), onEvent = {
                }, onPlay = { played += it.id }, preview = previewSlot(mutableListOf()))
            }
        }

        pressOk(hasTestTag("preview"))

        assertThat(played).isEqualTo(listOf(Fixtures.alJazeera.id))
    }

    @Test
    fun theSmallVideoIsRemovedAsSoonAsAChannelIsOpenedFullScreen() {
        val onScreen = mutableListOf<Int>()
        val state = stateWith().copy(previewChannel = Fixtures.alJazeera)
        compose.setContent {
            TilfazTheme { LiveScreen(state = state, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}, preview = previewSlot(onScreen)) }
        }
        compose.waitForIdle()
        assertThat(onScreen).isEqualTo(listOf(Fixtures.alJazeera.id))

        pressOk(hasText(Fixtures.beinSports1.name) and hasClickAction())

        // The account has one connection: the preview must be gone before the full screen player connects.
        assertThat(onScreen).isEqualTo(emptyList())
    }

    @Test
    fun theSmallVideoIsRemovedWhenASuggestionIsOpenedToo() {
        val onScreen = mutableListOf<Int>()
        val typing = stateWith().copy(query = "jaz", suggestions = listOf(Fixtures.alJazeera), previewChannel = Fixtures.alJazeera)
        compose.setContent {
            TilfazTheme {
                LiveScreen(state = typing, channels = pages().collectAsLazyPagingItems(), onEvent = {}, onPlay = {}, preview = previewSlot(onScreen))
            }
        }

        pressOk(hasText("AL JAZEERA HD") and hasClickAction())

        assertThat(onScreen).isEqualTo(emptyList())
    }

    private companion object {
        const val MIN_NAME_WIDTH_PX = 200f // 100dp at 2px per dp: enough for a readable, ellipsized channel name
    }
}
