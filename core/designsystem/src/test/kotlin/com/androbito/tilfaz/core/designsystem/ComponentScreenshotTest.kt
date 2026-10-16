package com.androbito.tilfaz.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androbito.tilfaz.core.designsystem.component.CategoryRow
import com.androbito.tilfaz.core.designsystem.component.ChannelRow
import com.androbito.tilfaz.core.designsystem.component.HeroTile
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests of the design system in its meaningful states (default, selected, with/without data).
 * These are the components every screen is built from, so pixel changes here are reviewed on purpose.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class ComponentScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun heroTiles() {
        compose.setContent {
            TilfazTheme {
                Row(Modifier.padding(Dimens.SpaceL), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL)) {
                    HeroTile("LIVE TV", Icons.Default.LiveTv, TilfazColors.LiveGradient, "Last updated: 43 min ago", false, {
                    }, {}, Modifier.width(360.dp).height(300.dp))
                    HeroTile("MOVIES", Icons.Default.Movie, TilfazColors.MoviesGradient, "Loading…", true, {}, {}, Modifier.width(360.dp).height(300.dp))
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun listRowsAndPoster() {
        compose.setContent {
            TilfazTheme {
                Row(Modifier.padding(Dimens.SpaceL), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL)) {
                    Column(Modifier.width(320.dp)) {
                        CategoryRow("ALL", 12339, selected = false, onClick = {})
                        CategoryRow("BEIN SPORTS HD", 63, selected = true, onClick = {})
                        ChannelRow("|AR| AL JAZEERA HD", null, "News Hour", 0.5f, selected = false, onClick = {})
                        ChannelRow("BEIN SPORTS 1 FHD", null, null, null, selected = true, onClick = {}, isFavorite = true)
                    }
                    PosterCard("|FR| L'Être Aimé (2026)", posterUrl = null, onClick = {}, ratingBadge = "7", progress = 0.3f)
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
