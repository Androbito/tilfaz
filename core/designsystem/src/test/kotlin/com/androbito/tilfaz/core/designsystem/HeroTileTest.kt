package com.androbito.tilfaz.core.designsystem

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.component.HeroTile
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class HeroTileTest {
    @get:Rule
    val compose = createComposeRule()

    private val refreshLabel get() = ApplicationProvider.getApplicationContext<Context>().getString(R.string.ds_refresh)
    private var opened = 0
    private var refreshed = 0

    private fun show() = compose.setContent {
        TilfazTheme {
            HeroTile(
                title = "Movies",
                icon = Icons.Default.Movie,
                gradient = SolidColor(androidx.compose.ui.graphics.Color.DarkGray),
                lastUpdatedLabel = "Last updated: 6 h ago",
                isSyncing = false,
                onClick = { opened++ },
                onRefresh = { refreshed++ }
            )
        }
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.pressOk() {
        performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    @Test
    fun theRefreshButtonCanBeReachedFromTheTileWithDown() {
        show()
        compose.onNode(hasContentDescription("Movies")).requestFocus()

        press(Key.DirectionDown)

        compose.onNodeWithContentDescription(refreshLabel).assertIsFocused()
    }

    @Test
    fun okOnTheRefreshButtonRefreshesWithoutOpeningTheSection() {
        show()
        compose.onNodeWithContentDescription(refreshLabel).requestFocus()

        compose.onNodeWithContentDescription(refreshLabel).pressOk()

        assertThat(refreshed).isEqualTo(1)
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun okOnTheTileOpensTheSectionWithoutRefreshing() {
        show()
        val tile = compose.onNode(hasContentDescription("Movies"))
        tile.requestFocus()

        tile.pressOk()

        assertThat(opened).isEqualTo(1)
        assertThat(refreshed).isEqualTo(0)
    }

    @Test
    fun upFromTheRefreshButtonGoesBackToTheTile() {
        show()
        compose.onNodeWithContentDescription(refreshLabel).requestFocus()

        press(Key.DirectionUp)

        compose.onNode(hasContentDescription("Movies")).assertIsFocused()
    }
}
