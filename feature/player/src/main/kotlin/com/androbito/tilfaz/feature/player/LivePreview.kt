package com.androbito.tilfaz.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.androbito.tilfaz.core.designsystem.component.ChannelLogo
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamSource
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.awaitCancellation

/**
 * The small live video shown beside the channel list. Pressing OK on it opens the channel full screen.
 *
 * It owns one light player (no audio, small picture) for as long as it is on screen, and releases it the moment it leaves or
 * the channel changes: the account allows a single connection, so this player and the full screen one must never overlap.
 */
@OptIn(UnstableApi::class)
@Composable
fun LivePreview(channel: LiveChannel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entry = remember(context) { EntryPointAccessors.fromApplication(context.applicationContext, PlayerEntryPoint::class.java) }
    val factory = remember(entry) { entry.exoPlayerFactory() }
    val buildSource = remember(entry) { entry.buildStreamSource() }
    LivePreviewPlayer(
        channel = channel,
        resolveSource = { buildSource(Playable.Live(it)).getOrNull() },
        createPlayer = factory::createPreview,
        onOpen = onOpen,
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Composable
internal fun LivePreviewPlayer(
    channel: LiveChannel,
    resolveSource: suspend (LiveChannel) -> StreamSource?,
    createPlayer: (StreamSource) -> ExoPlayer,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var showingVideo by remember(channel.id) { mutableStateOf(false) }

    // One effect per channel: it finds the stream address, makes the player, and keeps it until the effect is cancelled, which
    // happens when the channel changes or the video leaves the screen. Cancelling always releases it, so a player never
    // outlives what is shown and two of them never run side by side.
    LaunchedEffect(channel.id) {
        val source = resolveSource(channel) ?: return@LaunchedEffect
        val exo = createPlayer(source)
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                showingVideo = true
            }
        }
        exo.addListener(listener)
        exo.prepare()
        player = exo
        try {
            awaitCancellation()
        } finally {
            exo.removeListener(listener)
            exo.release()
            player = null
        }
    }

    FocusableSurface(onClick = onOpen, modifier = modifier, corner = 8.dp) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            player?.let { PlayerSurface(player = it, surfaceType = SURFACE_TYPE_SURFACE_VIEW, modifier = Modifier.fillMaxSize()) }
            // The logo stays until the first picture is drawn, so the box is never an empty black rectangle.
            if (!showingVideo) ChannelLogo(channel.logoUrl, Modifier.align(Alignment.Center).then(Modifier.fillMaxSize(LOGO_FRACTION)))
        }
    }
}

private const val LOGO_FRACTION = 0.4f
