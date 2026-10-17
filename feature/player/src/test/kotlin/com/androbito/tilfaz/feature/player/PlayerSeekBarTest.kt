package com.androbito.tilfaz.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class PlayerSeekBarTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int) = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private val half = 30 * 60 * 1_000L
    private val total = 2 * 60 * 60 * 1_000L

    @Test
    fun showsWhereYouAreAndHowLongItIs() {
        compose.setContent { TilfazTheme { PlayerSeekBar(positionMs = half, durationMs = total, scrub = null) } }

        compose.onNodeWithText("30:00").assertIsDisplayed()
        compose.onNodeWithText("2:00:00").assertIsDisplayed()
    }

    @Test
    fun whileScrubbingTheCursorShowsItsTargetAndTheChange() {
        val scrub = ScrubState(originMs = half, targetMs = half + 120_000, direction = +1, streak = 1, lastPressAtMs = 0)
        compose.setContent { TilfazTheme { PlayerSeekBar(positionMs = half, durationMs = total, scrub = scrub) } }

        compose.onNodeWithText("32:00").assertIsDisplayed()
        compose.onNodeWithText("+2:00").assertIsDisplayed()
    }

    @Test
    fun withoutAScrubThereIsNoChangeLabel() {
        compose.setContent { TilfazTheme { PlayerSeekBar(positionMs = half, durationMs = total, scrub = null) } }

        compose.onNodeWithText("+2:00").assertDoesNotExist()
    }

    @Test
    fun aFileThatCannotBeSoughtSaysSoInsteadOfOfferingACursor() {
        compose.setContent { TilfazTheme { PlayerSeekBar(positionMs = half, durationMs = total, scrub = null, seekable = false) } }

        compose.onNodeWithText(string(R.string.player_not_seekable)).assertIsDisplayed()
    }

    @Test
    fun aSeekableFileShowsNoSuchMessage() {
        compose.setContent { TilfazTheme { PlayerSeekBar(positionMs = half, durationMs = total, scrub = null) } }

        compose.onNodeWithText(string(R.string.player_not_seekable)).assertDoesNotExist()
    }
}
