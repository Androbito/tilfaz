package com.androbito.tilfaz.core.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androbito.tilfaz.core.designsystem.component.ScoreLine
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.Rating
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ScoreLineTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsFilledStarsAndTheScoreOutOfTen() {
        compose.setContent { TilfazTheme { ScoreLine(Rating(7.1)) } }

        compose.onNodeWithText("★★★☆☆").assertIsDisplayed()
        compose.onNodeWithText("7.1 / 10").assertIsDisplayed()
    }

    @Test
    fun aVeryLowScoreFillsNoStar() {
        compose.setContent { TilfazTheme { ScoreLine(Rating(1.0)) } }

        compose.onNodeWithText("☆☆☆☆☆").assertIsDisplayed()
        compose.onNodeWithText("1.0 / 10").assertIsDisplayed()
    }
}
