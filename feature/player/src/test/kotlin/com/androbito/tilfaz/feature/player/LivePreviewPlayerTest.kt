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
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import java.lang.reflect.Proxy
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
    private val factory = ExoPlayerFactory(context, OkHttpClient())

    private var created = 0
    private var released = 0
    private var opened = 0

    private fun sourceOf(channel: LiveChannel) =
        StreamSource("http://provider.test/live/${channel.id}.ts", Playable.Live(channel), isLive = true, userAgent = "test", format = StreamFormat.MPEG_TS)

    /** A real preview player that counts how many were made and how many were released. */
    private fun countedPlayer(source: StreamSource): ExoPlayer {
        val real = factory.createPreview(source)
        created++
        return Proxy.newProxyInstance(ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java)) { _, method, args ->
            if (method.name == "release") released++
            try {
                method.invoke(real, *(args ?: emptyArray()))
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw e.targetException
            }
        } as ExoPlayer
    }

    private fun show(channel: () -> LiveChannel, resolve: suspend (LiveChannel) -> StreamSource? = { sourceOf(it) }) = compose.setContent {
        TilfazTheme {
            LivePreviewPlayer(channel = channel(), resolveSource = resolve, createPlayer = ::countedPlayer, onOpen = { opened++ })
        }
    }

    @Test
    fun `one player is made for the channel and none before its stream address is known`() {
        show({ Fixtures.alJazeera }, resolve = { null })
        compose.waitForIdle()

        assertThat(created).isEqualTo(0)
    }

    @Test
    fun `a player is made once the stream address is known`() {
        show({ Fixtures.alJazeera })
        compose.waitForIdle()

        assertThat(created).isEqualTo(1)
        assertThat(released).isEqualTo(0)
    }

    @Test
    fun `the player is released when the small video leaves the screen`() {
        var visible by mutableStateOf(true)
        compose.setContent {
            TilfazTheme {
                if (visible) LivePreviewPlayer(Fixtures.alJazeera, { sourceOf(it) }, ::countedPlayer, { opened++ })
            }
        }
        compose.waitForIdle()
        assertThat(created).isEqualTo(1)

        compose.runOnIdle { visible = false }
        compose.waitForIdle()

        assertThat(released).isEqualTo(1)
    }

    @Test
    fun `a new channel releases the old player before making the next one`() {
        var channel by mutableStateOf(Fixtures.alJazeera)
        compose.setContent {
            TilfazTheme { LivePreviewPlayer(channel, { sourceOf(it) }, ::countedPlayer, { opened++ }) }
        }
        compose.waitForIdle()

        compose.runOnIdle { channel = Fixtures.beinSports1 }
        compose.waitForIdle()

        assertThat(created).isEqualTo(2)
        assertThat(released).isEqualTo(1) // the first; the second is still playing
    }

    @Test
    fun `a loading indicator is shown until the first picture is drawn`() {
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
