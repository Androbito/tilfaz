package com.androbito.tilfaz.feature.player

import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import assertk.assertions.isNull
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import com.androbito.tilfaz.core.model.SubtitleStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
@OptIn(ExperimentalTestApi::class)
class PlayerOptionsPanelTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun row(text: String) = hasText(text) and hasClickAction()

    private fun pressOk(matcher: SemanticsMatcher) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private val arabic = TrackOption("0:0", TrackKind.AUDIO, "Arabic", "5.1 · AC3", "ar", isSelected = true)
    private val english = TrackOption("1:0", TrackKind.AUDIO, "English", "Stereo · AAC", "en", isSelected = false)
    private val french = TrackOption("2:0", TrackKind.SUBTITLE, "French", null, "fr", isSelected = false)
    private val englishSubs = TrackOption("3:0", TrackKind.SUBTITLE, "English", null, "en", isSelected = true)

    private var chosenAudio: TrackOption? = null
    private var chosenSubtitle: TrackOption? = null
    private var subtitleChosen = false
    private var chosenStyle: SubtitleStyle? = null
    private var chosenSpeed: Float? = null
    private var chosenAspect: AspectMode? = null
    private var closed = false

    private fun show(subtitles: List<TrackOption> = listOf(french, englishSubs), initial: OptionsPage = OptionsPage.Root) {
        compose.setContent {
            var page by remember { mutableStateOf(initial) }
            TilfazTheme {
                PlayerOptionsPanel(
                    page = page,
                    onPage = { page = it },
                    onClose = { closed = true },
                    audio = listOf(arabic, english),
                    subtitles = subtitles,
                    onSelectAudio = { chosenAudio = it },
                    onSelectSubtitle = {
                        subtitleChosen = true
                        chosenSubtitle = it
                    },
                    style = SubtitleStyle(),
                    onStyle = { chosenStyle = it },
                    speed = 1f,
                    onSpeed = { chosenSpeed = it },
                    aspect = AspectMode.FIT,
                    onAspect = { chosenAspect = it }
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun theRootListShowsWhatIsPlayingNow() {
        show()

        compose.onNode(row(string(R.string.player_options_audio))).assertExists()
        compose.onNodeWithText("Arabic", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("English", useUnmergedTree = true).assertExists() // the subtitle currently on
    }

    @Test
    fun withSubtitlesOffTheRootSaysOff() {
        show(subtitles = listOf(french))

        compose.onNodeWithText(string(R.string.player_options_off), useUnmergedTree = true).assertExists()
    }

    @Test
    fun theAudioPageListsTheTracksAndPicksOne() {
        show()
        pressOk(row(string(R.string.player_options_audio)))

        compose.onNode(row("English")).assertExists()
        pressOk(row("English"))

        assertThat(chosenAudio).isEqualTo(english)
    }

    @Test
    fun theSubtitlePageStartsWithOffAndOffMeansNone() {
        show()
        pressOk(row(string(R.string.player_options_subtitles)))

        pressOk(row(string(R.string.player_options_off)))

        assertThat(subtitleChosen).isEqualTo(true)
        assertThat(chosenSubtitle).isNull()
    }

    @Test
    fun aSubtitleTrackCanBePicked() {
        show()
        pressOk(row(string(R.string.player_options_subtitles)))

        pressOk(row("French"))

        assertThat(chosenSubtitle).isEqualTo(french)
    }

    @Test
    fun aPageOpensOnTheTrackThatIsPlaying() {
        show(initial = OptionsPage.Audio)

        compose.onNode(row("Arabic")).assertIsFocused()
    }

    @Test
    fun backGoesFromAPageToTheRootAndThenClosesThePanel() {
        var dispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            var page by remember { mutableStateOf<OptionsPage>(OptionsPage.Audio) }
            TilfazTheme {
                PlayerOptionsPanel(
                    page = page,
                    onPage = { page = it },
                    onClose = { closed = true },
                    audio = listOf(arabic, english),
                    subtitles = emptyList(),
                    onSelectAudio = {},
                    onSelectSubtitle = {},
                    style = SubtitleStyle(),
                    onStyle = {},
                    speed = 1f,
                    onSpeed = {},
                    aspect = AspectMode.FIT,
                    onAspect = {}
                )
            }
        }
        compose.waitForIdle()

        compose.runOnIdle { dispatcher!!.onBackPressed() }
        compose.waitForIdle()
        compose.onNode(row(string(R.string.player_options_audio))).assertExists() // back on the root
        assertThat(closed).isEqualTo(false)

        compose.runOnIdle { dispatcher!!.onBackPressed() }
        assertThat(closed).isEqualTo(true)
    }

    @Test
    fun theStylePageShowsEachChoiceAndPressingOneMovesToTheNextValue() {
        show(initial = OptionsPage.Style)

        compose.onNode(row(string(R.string.player_options_style_size))).assertExists()
        pressOk(row(string(R.string.player_options_style_size)))
        assertThat(chosenStyle).isEqualTo(SubtitleStyle(size = SubtitleSize.LARGE))

        pressOk(row(string(R.string.player_options_style_color)))
        assertThat(chosenStyle).isEqualTo(SubtitleStyle(textColor = SubtitleColor.YELLOW))

        pressOk(row(string(R.string.player_options_style_background)))
        assertThat(chosenStyle).isEqualTo(SubtitleStyle(background = SubtitleBackground.BLACK))
    }

    @Test
    fun theSpeedPageOffersTheUsualSpeedsAndPicksOne() {
        show(initial = OptionsPage.Speed)

        pressOk(row("1.5x"))

        assertThat(chosenSpeed).isEqualTo(1.5f)
    }

    @Test
    fun theAspectPagePicksHowTheVideoFillsTheScreen() {
        show(initial = OptionsPage.Aspect)

        pressOk(row(string(R.string.player_aspect_fill)))

        assertThat(chosenAspect).isEqualTo(AspectMode.FILL)
    }

    @Test
    fun theRootLeadsToEveryPage() {
        show()

        compose.onNode(row(string(R.string.player_options_subtitle_style))).assertExists()
        compose.onNode(row(string(R.string.player_options_speed))).assertExists()
        compose.onNode(row(string(R.string.player_options_aspect))).assertExists()
    }
}
