package com.androbito.tilfaz.feature.player

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.R
import com.androbito.tilfaz.core.designsystem.component.ChannelLogo
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamSource
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.awaitCancellation

/**
 * The small live video shown beside the channel list. Pressing OK on it opens the channel full screen.
 *
 * The player behind it is the app's single [LivePlayerHolder] player: scrolling switches its channel, and opening a channel
 * full screen hands this same player over, so there is no second stream and no second decoder to wait for.
 */
@OptIn(UnstableApi::class)
@Composable
fun LivePreview(channel: LiveChannel?, handingOff: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entry = remember(context) { EntryPointAccessors.fromApplication(context.applicationContext, PlayerEntryPoint::class.java) }
    val holder = remember(entry) { entry.livePlayerHolder() }
    val buildSource = remember(entry) { entry.buildStreamSource() }
    LivePreviewPlayer(
        channel = channel,
        handingOff = handingOff,
        resolveSource = { buildSource(Playable.Live(it)).getOrNull() },
        holder = holder,
        onOpen = onOpen,
        modifier = modifier
    )
}

/**
 * [channel] is the one to play, or null while the user is scrolling (the player is paused, not rebuilt). [handingOff] is true
 * once a channel has been opened full screen: the player then outlives this composable.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun LivePreviewPlayer(
    channel: LiveChannel?,
    handingOff: Boolean,
    resolveSource: suspend (LiveChannel) -> StreamSource?,
    holder: LivePlayerHolder,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var showingVideo by remember(channel?.id) { mutableStateOf(false) }
    val handingOffNow by rememberUpdatedState(handingOff)

    if (handingOff) SideEffect { holder.markHandingOff() }
    DisposableEffect(Unit) {
        onDispose {
            // A hand off keeps the player alive for the full screen player; any other way out frees the stream.
            if (!handingOffNow) holder.release()
        }
    }

    // One effect per channel: it finds the stream address and has the shared player play it, and only listens while it is on.
    LaunchedEffect(channel?.id, handingOff) {
        if (handingOff) return@LaunchedEffect
        if (channel == null) {
            holder.pause()
            return@LaunchedEffect
        }
        val shownAt = SystemClock.elapsedRealtime()
        val source = resolveSource(channel) ?: return@LaunchedEffect
        val exo = holder.show(source)
        // The stream is already on screen (the player was parked and picked up again): no first picture is coming to announce it.
        if (holder.resumedSameStream && exo.playbackState == Player.STATE_READY) showingVideo = true
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && holder.resumedSameStream) showingVideo = true
            }

            override fun onRenderedFirstFrame() {
                showingVideo = true
                // Timings only (channel number and milliseconds), to tell a slow provider from a slow decoder on a real TV.
                Log.d(TAG, "channel ${channel.id}: first picture ${SystemClock.elapsedRealtime() - shownAt} ms after the preview was asked for")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.d(TAG, "channel ${channel.id}: error ${error.errorCodeName} after ${SystemClock.elapsedRealtime() - shownAt} ms")
            }
        }
        exo.addListener(listener)
        player = exo
        try {
            awaitCancellation()
        } finally {
            exo.removeListener(listener)
        }
    }

    FocusableSurface(onClick = onOpen, modifier = modifier, corner = 8.dp) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            // A TextureView, not a SurfaceView: a SurfaceView inside this scaled, re-composed pane got its surface only at the
            // next redraw (up to 30 s later). The cost of the extra copy at this size is nil.
            // No picture while scrolling: the box shows the channel's logo, and the player waits for the next channel.
            if (channel != null) player?.let { PlayerSurface(player = it, surfaceType = SURFACE_TYPE_TEXTURE_VIEW, modifier = Modifier.fillMaxSize()) }
            if (channel != null && !showingVideo) {
                ChannelLogo(channel.logoUrl, Modifier.align(Alignment.Center).fillMaxSize(LOGO_FRACTION))
                // Connecting can take a few seconds on a weak TV; say so, or the box looks stuck.
                Text(
                    stringResource(R.string.ds_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = TilfazColors.OnSurfaceMuted,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                )
            }
        }
    }
}

private const val LOGO_FRACTION = 0.4f
private const val TAG = "LivePreview"
