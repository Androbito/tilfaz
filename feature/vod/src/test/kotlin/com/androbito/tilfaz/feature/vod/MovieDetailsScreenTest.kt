package com.androbito.tilfaz.feature.vod

import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
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
    fun theBackKeyOnAFocusedButtonGoesBackOnTheFirstPress() {
        var backs = 0
        compose.setContent {
            BackHandler { backs++ }
            TilfazTheme {
                MovieDetailsScreen(state = MovieDetailsUiState(movie = Fixtures.movie), onEvent = {}, onPlay = {}, onOpenMovie = {})
            }
        }
        val play = hasText("Play") and hasClickAction()
        compose.onNode(play).requestFocus()

        compose.onNode(play).performKeyInput { pressKey(Key.Back) }

        assertThat(backs).isEqualTo(1)
    }
}
