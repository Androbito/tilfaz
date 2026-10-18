package com.androbito.tilfaz.feature.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import java.time.Duration
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LivePlayerHolderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val factory = ExoPlayerFactory(context, OkHttpClient())
    private val holder = LivePlayerHolder(factory)

    private fun sourceOf(channel: LiveChannel) =
        StreamSource("http://provider.test/live/${channel.id}.ts", Playable.Live(channel), isLive = true, userAgent = "test", format = StreamFormat.MPEG_TS)

    private val first = sourceOf(Fixtures.alJazeera)
    private val second = sourceOf(Fixtures.beinSports1)

    private fun passTime(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun `showing another channel switches the same player instead of making a new one`() {
        val a = holder.show(first)
        val b = holder.show(second)

        assertThat(b).isSameInstanceAs(a)
        assertThat(holder.created).isEqualTo(1)
        assertThat(b.currentMediaItem!!.localConfiguration!!.uri.toString()).isEqualTo(second.url)
        holder.release()
    }

    @Test
    fun `showing the channel that is already playing leaves the stream alone`() {
        val player = holder.show(first)
        val item = player.currentMediaItem

        holder.show(first)

        assertThat(player.currentMediaItem).isSameInstanceAs(item)
        holder.release()
    }

    @Test
    fun `pausing keeps the player, and showing again resumes it`() {
        val player = holder.show(first)

        holder.pause()
        assertThat(player.playWhenReady).isEqualTo(false)
        assertThat(holder.created).isEqualTo(1)

        holder.show(first)
        assertThat(player.playWhenReady).isEqualTo(true)
        holder.release()
    }

    @Test
    fun `resuming a paused preview never seeks, because a seek reconnects a live stream and the provider holds the old slot`() {
        val player = holder.show(first)
        var seeks = 0
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int
            ) {
                if (reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK) seeks++
            }
        })
        holder.pause()

        holder.show(first)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(seeks).isEqualTo(0)
        holder.release()
    }

    @Test
    fun `showing the same stream again says it was resumed, a new channel says it was not`() {
        holder.show(first)
        assertThat(holder.resumedSameStream).isEqualTo(false)

        holder.show(first)
        assertThat(holder.resumedSameStream).isEqualTo(true)

        holder.show(second)
        assertThat(holder.resumedSameStream).isEqualTo(false)
        holder.release()
    }

    @Test
    fun `releasing frees the player and the next show makes a fresh one`() {
        holder.show(first)

        holder.release()
        holder.show(first)

        assertThat(holder.released).isEqualTo(1)
        assertThat(holder.created).isEqualTo(2)
        holder.release()
    }

    @Test
    fun `the full screen player takes the very same player for the same channel, with sound and the full picture`() {
        val preview = holder.show(first)

        val taken = holder.take(first, PlaybackSettings())

        assertThat(taken).isSameInstanceAs(preview)
        assertThat(taken!!.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)).isEqualTo(false)
        assertThat(taken.trackSelectionParameters.maxVideoWidth).isEqualTo(Int.MAX_VALUE)
        assertThat(holder.created).isEqualTo(1)
        assertThat(holder.released).isEqualTo(0)
        taken.release()
    }

    @Test
    fun `once taken, the holder no longer owns the player`() {
        holder.show(first)
        holder.take(first, PlaybackSettings())!!

        assertThat(holder.take(first, PlaybackSettings())).isNull()
        holder.release()
        assertThat(holder.released).isEqualTo(0)
    }

    @Test
    fun `a different channel cannot take the player, which is released so two streams never overlap`() {
        holder.show(first)

        val taken = holder.take(second, PlaybackSettings())

        assertThat(taken).isNull()
        assertThat(holder.released).isEqualTo(1)
    }

    @Test
    fun `nothing to take when nothing is parked`() {
        assertThat(holder.take(first, PlaybackSettings())).isNull()
    }

    @Test
    fun `a player given back is paused and parked, and the preview picks it up silent and small`() {
        val player = holder.show(first)
        val taken = holder.take(first, PlaybackSettings())!!

        holder.retire(taken)
        assertThat(player.playWhenReady).isEqualTo(false)

        val again = holder.show(first)
        assertThat(again).isSameInstanceAs(player)
        assertThat(again.playWhenReady).isEqualTo(true)
        assertThat(again.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)).isEqualTo(true)
        assertThat(again.trackSelectionParameters.maxVideoWidth).isEqualTo(ExoPlayerFactory.PREVIEW_MAX_WIDTH)
        assertThat(holder.created).isEqualTo(1)
        holder.release()
    }

    @Test
    fun `a player given back and not picked up soon is released, so it does not hold the connection`() {
        val taken = holder.take(first, PlaybackSettings()) ?: holder.show(first).let { holder.take(first, PlaybackSettings())!! }
        holder.retire(taken)

        passTime(LivePlayerHolder.PARK_MS - 1)
        assertThat(holder.released).isEqualTo(0)
        passTime(2)

        assertThat(holder.released).isEqualTo(1)
    }

    @Test
    fun `a hand off that is never completed does not keep the stream forever`() {
        holder.show(first)

        holder.markHandingOff()
        passTime(LivePlayerHolder.HAND_OFF_MS + 1)

        assertThat(holder.released).isEqualTo(1)
    }

    @Test
    fun `taking the player completes the hand off, so nothing expires afterwards`() {
        holder.show(first)
        holder.markHandingOff()
        val taken = holder.take(first, PlaybackSettings())

        passTime(LivePlayerHolder.HAND_OFF_MS + 1_000)

        assertThat(taken).isNotNull()
        assertThat(holder.released).isEqualTo(0)
        taken!!.release()
    }

    @Test
    fun `marking a hand off with nothing playing does nothing`() {
        holder.markHandingOff()

        passTime(LivePlayerHolder.HAND_OFF_MS + 1_000)

        assertThat(holder.released).isEqualTo(0)
    }

    @Test
    fun `marking a hand off twice does not push its deadline back`() {
        holder.show(first)

        holder.markHandingOff()
        passTime(LivePlayerHolder.HAND_OFF_MS - 1_000)
        holder.markHandingOff()
        passTime(1_100)

        assertThat(holder.released).isEqualTo(1)
    }
}
