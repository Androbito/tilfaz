package com.androbito.tilfaz.feature.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@UnstableApi
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class LivePreviewPlayerTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val holder = LivePlayerHolder(ExoPlayerFactory(context, OkHttpClient()))
    private var opened = 0

    private fun sourceOf(channel: LiveChannel) =
        StreamSource("http://provider.test/live/${channel.id}.ts", Playable.Live(channel), isLive = true, userAgent = "test", format = StreamFormat.MPEG_TS)

    private fun show(channel: () -> LiveChannel?, handingOff: () -> Boolean = { false }, resolve: suspend (LiveChannel) -> StreamSource? = { sourceOf(it) }) =
        compose.setContent {
            TilfazTheme { LivePreviewPlayer(channel(), handingOff(), resolve, holder, { opened++ }) }
        }

    @Test
    fun `no player is made before the stream address is known`() {
        show({ Fixtures.alJazeera }, resolve = { null })
        compose.waitForIdle()

        assertThat(holder.created).isEqualTo(0)
    }

    @Test
    fun `a player is made once the stream address is known`() {
        show({ Fixtures.alJazeera })
        compose.waitForIdle()

        assertThat(holder.created).isEqualTo(1)
        assertThat(holder.released).isEqualTo(0)
    }

    @Test
    fun `another channel reuses the same player instead of making a new one`() {
        var channel by mutableStateOf(Fixtures.alJazeera)
        compose.setContent { TilfazTheme { LivePreviewPlayer(channel, false, { sourceOf(it) }, holder, { opened++ }) } }
        compose.waitForIdle()
        val player = holder.activePlayer

        compose.runOnIdle { channel = Fixtures.beinSports1 }
        compose.waitForIdle()

        assertThat(holder.created).isEqualTo(1)
        assertThat(holder.released).isEqualTo(0)
        assertThat(holder.activePlayer).isEqualTo(player)
        assertThat(player!!.currentMediaItem!!.localConfiguration!!.uri.toString()).isEqualTo(sourceOf(Fixtures.beinSports1).url)
    }

    @Test
    fun `while scrolling the player is paused, not released`() {
        var channel by mutableStateOf<LiveChannel?>(Fixtures.alJazeera)
        compose.setContent { TilfazTheme { LivePreviewPlayer(channel, false, { sourceOf(it) }, holder, { opened++ }) } }
        compose.waitForIdle()

        compose.runOnIdle { channel = null }
        compose.waitForIdle()

        assertThat(holder.released).isEqualTo(0)
        assertThat(holder.activePlayer!!.playWhenReady).isEqualTo(false)
    }

    @Test
    fun `the player is released when the small video leaves the screen`() {
        var visible by mutableStateOf(true)
        compose.setContent { TilfazTheme { if (visible) LivePreviewPlayer(Fixtures.alJazeera, false, { sourceOf(it) }, holder, { opened++ }) } }
        compose.waitForIdle()

        compose.runOnIdle { visible = false }
        compose.waitForIdle()

        assertThat(holder.released).isEqualTo(1)
        assertThat(holder.activePlayer).isNull()
    }

    @Test
    fun `leaving the screen while a channel is opened full screen keeps the player for the full screen player`() {
        var visible by mutableStateOf(true)
        var handingOff by mutableStateOf(false)
        compose.setContent {
            TilfazTheme { if (visible) LivePreviewPlayer(null.takeIf { handingOff } ?: Fixtures.alJazeera, handingOff, { sourceOf(it) }, holder, { opened++ }) }
        }
        compose.waitForIdle()

        compose.runOnIdle { handingOff = true }
        compose.waitForIdle()
        compose.runOnIdle { visible = false }
        compose.waitForIdle()

        assertThat(holder.released).isEqualTo(0)
        assertThat(holder.activePlayer).isNotNull()
        holder.release()
    }

    @Test
    fun `a loading label is shown until the first picture is drawn`() {
        show({ Fixtures.alJazeera })
        compose.waitForIdle()

        compose.onNodeWithText(context.getString(com.androbito.tilfaz.core.designsystem.R.string.ds_loading)).assertExists()
    }

    @Test
    fun `okay on the small video opens it`() {
        show({ Fixtures.alJazeera })
        compose.waitForIdle()

        compose.onNode(hasClickAction()).requestFocus()
        compose.onNode(hasClickAction()).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertThat(opened).isEqualTo(1)
    }
}
