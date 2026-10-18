package com.androbito.tilfaz.feature.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one player behind the small live video, shared with the full screen player.
 *
 * Creating a video decoder is slow on a TV (many seconds on some), and the account allows a single stream. So there is a
 * single player: scrolling switches its channel without rebuilding it, opening a channel full screen hands this very player
 * to the full screen screen (sound on, full picture, no restart), and going back hands it to the small video again.
 * Everything runs on the main thread.
 */
@UnstableApi
@Singleton
class LivePlayerHolder @Inject constructor(private val factory: ExoPlayerFactory) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var url: String? = null
    private var isUpgraded = false
    private var expiry: Runnable? = null

    internal var created = 0
        private set
    internal var released = 0
        private set

    internal val activePlayer: ExoPlayer? get() = player

    /** True when the last [show] found the player already on that stream, so there is no new first picture to wait for. */
    internal var resumedSameStream = false
        private set

    /** The player showing [source] as a silent small preview: the same player as before when there is one. */
    fun show(source: StreamSource): ExoPlayer {
        cancelExpiry()
        val reusable = player?.takeIf { it.playerError == null }
        if (reusable == null) releaseNow()
        val exo = reusable ?: factory.createPreview(source).also {
            player = it
            url = source.url
            created++
            resumedSameStream = false
            return it
        }
        if (isUpgraded) {
            factory.downgradeToPreview(exo)
            isUpgraded = false
        }
        if (url != source.url) {
            exo.setMediaItem(factory.previewMediaItem(source))
            exo.prepare()
            url = source.url
            resumedSameStream = false
        } else {
            // Already on this stream: just play on. A seek would reconnect a live stream, and the provider keeps the old
            // connection busy for a while, which is exactly the many second wait this player exists to avoid.
            resumedSameStream = true
        }
        exo.playWhenReady = true
        return exo
    }

    /** Scrolling: no channel is being previewed. The player is kept, because building the next one is the slow part. */
    fun pause() {
        player?.playWhenReady = false
    }

    /** The small video left the screen for good: free the player and with it the stream. */
    fun release() {
        cancelExpiry()
        releaseNow()
    }

    /** A channel is being opened full screen: the player must survive the small video leaving, but not for ever. */
    fun markHandingOff() {
        if (player == null || expiry != null) return
        scheduleExpiry(HAND_OFF_MS)
    }

    /** The full screen player asks for the player of [source]. A player on another channel is released: one stream only. */
    fun take(source: StreamSource, settings: PlaybackSettings): ExoPlayer? {
        cancelExpiry()
        val exo = player ?: return null
        val matches = url == source.url && exo.playerError == null
        player = null
        url = null
        if (!matches) {
            exo.release()
            released++
            return null
        }
        factory.upgradeToFull(exo, settings)
        isUpgraded = false
        return exo
    }

    /** The full screen player is done with its live player: park it, paused, for the small video to pick up. */
    fun retire(exo: ExoPlayer) {
        releaseNow()
        if (exo.playerError != null) {
            exo.release()
            released++
            return
        }
        exo.playWhenReady = false
        player = exo
        url = exo.currentMediaItem?.localConfiguration?.uri?.toString()
        isUpgraded = true
        scheduleExpiry(PARK_MS)
    }

    private fun releaseNow() {
        player?.release()
        if (player != null) released++
        player = null
        url = null
        isUpgraded = false
    }

    private fun scheduleExpiry(afterMs: Long) {
        cancelExpiry()
        val task = Runnable {
            expiry = null
            releaseNow()
        }
        expiry = task
        handler.postDelayed(task, afterMs)
    }

    private fun cancelExpiry() {
        expiry?.let(handler::removeCallbacks)
        expiry = null
    }

    internal companion object {
        /** How long a player handed back waits for the small video to pick it up; shorter than the provider's slot hold. */
        const val PARK_MS = 3_000L

        /** How long a hand off may take before it is given up. */
        const val HAND_OFF_MS = 10_000L
    }
}
