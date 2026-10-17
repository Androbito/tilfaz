package com.androbito.tilfaz.feature.player

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.PlaybackSettings
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
        shadowOf(player.applicationLooper).idle()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        assertThat(shadowOf(audioManager).lastAudioFocusRequest).isNotNull()
        player.release()
    }
}
