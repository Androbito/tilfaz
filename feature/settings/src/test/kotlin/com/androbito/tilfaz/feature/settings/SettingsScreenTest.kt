package com.androbito.tilfaz.feature.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var state by mutableStateOf(SettingsUiState())

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun button(textId: Int) = hasText(string(textId)) and hasClickAction()

    /** A remote's OK key on a focused node: TV Material surfaces are driven by keys, not by touch. */
    @OptIn(ExperimentalTestApi::class)
    private fun pressOk(matcher: SemanticsMatcher) {
        compose.onNode(matcher).requestFocus()
        compose.onNode(matcher).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private val events = mutableListOf<SettingsEvent>()

    private fun show() = compose.setContent {
        TilfazTheme {
            SettingsScreen(
                state = state,
                onEvent = { event ->
                    events += event
                    state = when (event) {
                        SettingsEvent.SignOutRequested -> state.copy(confirmSignOut = true)
                        SettingsEvent.SignOutCancelled -> state.copy(confirmSignOut = false)
                        else -> state
                    }
                }
            )
        }
    }

    @Test
    fun askingToSignOutPutsFocusOnTheSafeChoice() {
        show()

        pressOk(button(R.string.settings_sign_out))

        compose.onNode(button(R.string.settings_cancel)).assertIsFocused()
    }

    @Test
    fun cancellingGivesFocusBackToSignOut() {
        show()
        pressOk(button(R.string.settings_sign_out))

        pressOk(button(R.string.settings_cancel))

        compose.onNode(button(R.string.settings_sign_out)).assertIsFocused()
    }

    @Test
    fun refreshingTheAccountSaysSo() {
        state = SettingsUiState(isRefreshingStatus = true)
        show()

        compose.onNodeWithText(string(R.string.settings_account_refreshing)).assertExists()
    }

    @Test
    fun theLanguageRowsAreSeparateChoicesForAudioAndSubtitles() {
        show()

        // Both rows list the same languages; the audio row comes first on the page.
        val english = hasText(string(R.string.settings_lang_english)) and hasClickAction()
        compose.onAllNodes(english)[0].requestFocus()
        compose.onAllNodes(english)[0].performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onAllNodes(english)[1].requestFocus()
        compose.onAllNodes(english)[1].performKeyInput { pressKey(Key.DirectionCenter) }

        assertThat(events).isEqualTo(listOf(SettingsEvent.SetAudioLanguage("en"), SettingsEvent.SetSubtitleLanguage("en")))
    }

    @Test
    fun subtitleSizeColourAndBackgroundCanBeChosenHere() {
        show()

        pressOk(button(R.string.settings_size_large))
        pressOk(button(R.string.settings_color_yellow))
        pressOk(button(R.string.settings_background_blue))

        assertThat(events).isEqualTo(
            listOf(
                SettingsEvent.SetSubtitleSize(SubtitleSize.LARGE),
                SettingsEvent.SetSubtitleColor(SubtitleColor.YELLOW),
                SettingsEvent.SetSubtitleBackground(SubtitleBackground.BLUE)
            )
        )
    }

    @Test
    fun theCatalogsCanBeRefreshedByHand() {
        show()

        pressOk(button(R.string.settings_catalog_refresh))

        assertThat(events).isEqualTo(listOf(SettingsEvent.RefreshCatalogs))
    }
}
