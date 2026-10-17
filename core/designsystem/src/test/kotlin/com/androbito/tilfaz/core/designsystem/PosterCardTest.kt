package com.androbito.tilfaz.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class PosterCardTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theHeightFollowsTheWidthSoANarrowPosterIsNotStretched() {
        compose.setContent {
            TilfazTheme { Box(Modifier.width(140.dp)) { PosterCard("A movie", posterUrl = null, onClick = {}, modifier = Modifier.testTag("poster")) } }
        }

        val bounds = compose.onNodeWithTag("poster").fetchSemanticsNode().boundsInRoot
        val expectedRatio = Dimens.PosterWidth.value / Dimens.PosterHeight.value
        assertThat(bounds.width / bounds.height).isCloseTo(expectedRatio, delta = 0.01f)
    }
}
