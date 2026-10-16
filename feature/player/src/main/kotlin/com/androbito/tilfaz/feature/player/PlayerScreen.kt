package com.androbito.tilfaz.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.ProgressBar
import com.androbito.tilfaz.core.designsystem.component.formatPlayerTime
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.StreamFormat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

fun NavGraphBuilder.playerScreen(onExit: () -> Unit) {
    composable<PlayerRoute> { PlayerRoute(onExit) }
}

@Composable
fun PlayerRoute(onExit: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(state = state, onEvent = viewModel::onEvent, onExit = onExit)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PlayerEntryPoint {
    @OptIn(UnstableApi::class)
    fun exoPlayerFactory(): ExoPlayerFactory
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(state: PlayerUiState, onEvent: (PlayerEvent) -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val factory = remember(context) { EntryPointAccessors.fromApplication(context.applicationContext, PlayerEntryPoint::class.java).exoPlayerFactory() }
    val source = state.source

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsShownAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun showControls() {
        controlsVisible = true
        controlsShownAt = System.currentTimeMillis()
    }

    // (Re)create the player whenever the source changes: channel zap, format switch, retry.
    DisposableEffect(source) {
        if (source == null) return@DisposableEffect onDispose { }
        val exo = factory.create(source, state.settings)
        if (state.startPositionMs > 0) exo.seekTo(state.startPositionMs)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) durationMs = exo.duration.coerceAtLeast(0)
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) { onEvent(PlayerEvent.PlaybackFailed(error.toPlayerError())) }
        }
        exo.addListener(listener)
        exo.prepare()
        player = exo
        showControls()
        onDispose {
            exo.removeListener(listener)
            exo.release()
            player = null
        }
    }

    // Position ticker: drives the progress bar, the saved resume position and the auto-hide of controls.
    LaunchedEffect(player) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            positionMs = exo.currentPosition.coerceAtLeast(0)
            if (durationMs <= 0) durationMs = exo.duration.coerceAtLeast(0)
            if (!state.isLive && durationMs > 0) onEvent(PlayerEvent.ProgressTick(positionMs, durationMs))
            if (controlsVisible && isPlaying && System.currentTimeMillis() - controlsShownAt > CONTROLS_TIMEOUT_MS) controlsVisible = false
            delay(1_000L)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || state.error != null) return@onKeyEvent false
                val exo = player
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.MediaPlayPause -> {
                        if (state.isLive) showControls() else { exo?.let { if (it.isPlaying) it.pause() else it.play() }; showControls() }
                        true
                    }
                    Key.MediaPlay -> { exo?.play(); true }
                    Key.MediaPause -> { exo?.pause(); true }
                    Key.DirectionLeft, Key.MediaRewind -> if (!state.isLive && exo != null) { exo.seekTo((exo.currentPosition - SEEK_STEP_MS).coerceAtLeast(0)); showControls(); true } else false
                    Key.DirectionRight, Key.MediaFastForward -> if (!state.isLive && exo != null) { exo.seekTo(exo.currentPosition + SEEK_STEP_MS); showControls(); true } else false
                    Key.DirectionUp, Key.ChannelUp -> if (state.isLive) { onEvent(PlayerEvent.ZapNext); true } else false
                    Key.DirectionDown, Key.ChannelDown -> if (state.isLive) { onEvent(PlayerEvent.ZapPrevious); true } else false
                    else -> false
                }
            },
    ) {
        player?.let { PlayerSurface(player = it, surfaceType = SURFACE_TYPE_SURFACE_VIEW, modifier = Modifier.fillMaxSize()) }

        when {
            state.error != null -> ErrorOverlay(state, onEvent, onExit)
            state.isLoading || (isBuffering && !isPlaying) -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState(label = stringResource(if (state.isLoading) R.string.player_loading else R.string.player_buffering))
            }
        }

        AnimatedVisibility(visible = controlsVisible && state.error == null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            InfoOverlay(state, positionMs, durationMs, isPlaying)
        }
    }
}

@Composable
private fun InfoOverlay(state: PlayerUiState, positionMs: Long, durationMs: Long, isPlaying: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            if (state.isLive) {
                Text(stringResource(R.string.player_live), style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(TilfazColors.Danger).padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Text(state.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
            if (!state.isLive && !isPlaying) Text("⏸", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
        if (state.isLive) {
            state.nowNext.now?.let { Text("${stringResource(R.string.player_now)}: ${it.title}", style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1) }
            state.nowNext.next?.let { Text("${stringResource(R.string.player_next)}: ${it.title}", style = MaterialTheme.typography.bodyLarge, color = TilfazColors.OnSurfaceMuted, maxLines = 1) }
        } else if (durationMs > 0) {
            ProgressBar(positionMs.toFloat() / durationMs, Modifier.fillMaxWidth().height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatPlayerTime(positionMs), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(formatPlayerTime(durationMs), style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
        Text(stringResource(if (state.isLive) R.string.player_hint_live else R.string.player_hint_vod), style = MaterialTheme.typography.bodySmall, color = TilfazColors.OnSurfaceMuted)
    }
}

@Composable
private fun ErrorOverlay(state: PlayerUiState, onEvent: (PlayerEvent) -> Unit, onExit: () -> Unit) {
    val error = state.error ?: return
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(error) { retryFocus.requestFocus() }
    Column(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.player_error_title), style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(
            text = when (error.kind) {
                PlayerErrorKind.SOURCE -> stringResource(R.string.player_error_source)
                PlayerErrorKind.NETWORK -> stringResource(R.string.player_error_network)
                PlayerErrorKind.DECODER -> stringResource(R.string.player_error_decoder)
                PlayerErrorKind.NOT_FOUND -> stringResource(R.string.player_error_not_found)
                PlayerErrorKind.UNKNOWN -> stringResource(R.string.player_error_unknown, error.detail ?: "?")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = TilfazColors.OnSurfaceMuted,
            modifier = Modifier.padding(top = Dimens.SpaceS, bottom = Dimens.SpaceL),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            Button(onClick = { onEvent(PlayerEvent.Retry) }, modifier = Modifier.focusRequester(retryFocus)) { Text(stringResource(R.string.player_retry)) }
            if (state.canSwitchFormat) {
                OutlinedButton(onClick = { onEvent(PlayerEvent.SwitchFormat) }) {
                    Text(stringResource(if (state.alternativeFormat == StreamFormat.HLS) R.string.player_switch_to_hls else R.string.player_switch_to_ts))
                }
            }
            OutlinedButton(onClick = onExit) { Text(stringResource(R.string.player_back)) }
        }
        error.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TilfazColors.OnSurfaceMuted, modifier = Modifier.padding(top = Dimens.SpaceL)) }
    }
}

private const val CONTROLS_TIMEOUT_MS = 5_000L
private const val SEEK_STEP_MS = 10_000L
