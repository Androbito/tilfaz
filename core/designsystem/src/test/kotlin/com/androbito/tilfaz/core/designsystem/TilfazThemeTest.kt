package com.androbito.tilfaz.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.LocalContentColor
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TilfazThemeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun textWithoutExplicitColorIsReadableOnTheDarkBackground() {
        var contentColor = Color.Unspecified
        compose.setContent { TilfazTheme { contentColor = LocalContentColor.current } }
        assertThat(contentColor).isEqualTo(TilfazColors.OnBackground)
    }
}
