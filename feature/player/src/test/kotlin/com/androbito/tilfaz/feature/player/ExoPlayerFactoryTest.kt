package com.androbito.tilfaz.feature.player

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ExoPlayerFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val source = StreamSource("http://provider.test/movie/1.mkv", Playable.Film(Fixtures.movie), isLive = false, userAgent = "test", format = null)

    @Test
    fun `the player declares itself as movie audio so the system knows what is playing`() {
        val player = ExoPlayerFactory(context, OkHttpClient()).create(source, PlaybackSettings())

        assertThat(player.audioAttributes.usage).isEqualTo(C.USAGE_MEDIA)
        assertThat(player.audioAttributes.contentType).isEqualTo(C.AUDIO_CONTENT_TYPE_MOVIE)
        player.release()
    }

    @Test
    fun `the player asks for audio focus when it starts, so something else taking it pauses the film instead of silencing it`() {
        val player = ExoPlayerFactory(context, OkHttpClient()).create(source, PlaybackSettings())

        player.prepare()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        player.settle(5_000) { shadowOf(audioManager).lastAudioFocusRequest != null }

        assertThat(shadowOf(audioManager).lastAudioFocusRequest).isNotNull()
        player.release()
    }

    /** The player works on its own thread, so what it does shows up a little later: keep the main thread turning for [millis]. */
    private fun ExoPlayer.settle(millis: Long, until: () -> Boolean = { false }) {
        val end = System.currentTimeMillis() + millis
        while (!until() && System.currentTimeMillis() < end) {
            shadowOf(applicationLooper).idle()
            Thread.sleep(10)
        }
    }

    private val live =
        StreamSource("http://provider.test/live/1.ts", Playable.Live(Fixtures.alJazeera), isLive = true, userAgent = "test", format = StreamFormat.MPEG_TS)

    @Test
    fun `a preview has no audio track at all, so it can never make a sound`() {
        val player = ExoPlayerFactory(context, OkHttpClient()).createPreview(live)

        assertThat(player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)).isEqualTo(true)
        assertThat(player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)).isEqualTo(true)
        player.release()
    }

    @Test
    fun `a preview decodes a small picture, not the full one`() {
        val player = ExoPlayerFactory(context, OkHttpClient()).createPreview(live)

        assertThat(player.trackSelectionParameters.maxVideoWidth).isEqualTo(ExoPlayerFactory.PREVIEW_MAX_WIDTH)
        assertThat(player.trackSelectionParameters.maxVideoHeight).isEqualTo(ExoPlayerFactory.PREVIEW_MAX_HEIGHT)
        player.release()
    }

    @Test
    fun `a preview never asks for audio focus, so it cannot pause or silence the full screen player`() {
        val player = ExoPlayerFactory(context, OkHttpClient()).createPreview(live)

        player.prepare()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        player.settle(1_000) // long enough for a full screen player to have asked, as the test above shows

        assertThat(shadowOf(audioManager).lastAudioFocusRequest).isNull()
        player.release()
    }

    @Test
    fun `a preview starts playing by itself`() {
        val player = ExoPlayerFactory(context, OkHttpClient()).createPreview(live)

        assertThat(player.playWhenReady).isEqualTo(true)
        player.release()
    }
}
