package com.androbito.tilfaz.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.TilfazButton
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.domain.usecase.BuildStreamSourceUseCase
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

    fun buildStreamSource(): BuildStreamSourceUseCase
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
    var scrub by remember { mutableStateOf<ScrubState?>(null) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var cues by remember { mutableStateOf(CueGroup.EMPTY_TIME_ZERO) }
    var optionsOpen by remember { mutableStateOf(false) }
    var seekable by remember { mutableStateOf(true) }
    var optionsPage by remember { mutableStateOf<OptionsPage>(OptionsPage.Root) }
    var speed by remember { mutableFloatStateOf(1f) }
    var aspect by remember { mutableStateOf(AspectMode.FIT) }

    fun showControls() {
        controlsVisible = true
        controlsShownAt = System.currentTimeMillis()
    }

    // (Re)create the player whenever the source changes: channel zap, format switch, retry.
    DisposableEffect(source) {
        if (source == null) return@DisposableEffect onDispose { }
        val exo = factory.create(source, state.settings)
        if (state.startPositionMs > 0) exo.seekTo(state.startPositionMs)
        exo.setPlaybackSpeed(speed) // a zap or retry keeps the speed the user chose
        var preferenceChecked = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) durationMs = exo.duration.coerceAtLeast(0)
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
                // Once per playback, so a track the user picks afterwards is never overridden.
                if (!preferenceChecked && newTracks.groups.any { it.type == C.TRACK_TYPE_AUDIO || it.type == C.TRACK_TYPE_TEXT }) {
                    preferenceChecked = true
                    listOf(TrackKind.AUDIO to state.settings.audioLanguage, TrackKind.SUBTITLE to state.settings.subtitleLanguage)
                        .forEach { (kind, language) ->
                            newTracks.preferredTrack(kind, language)?.let {
                                exo.trackSelectionParameters = exo.trackSelectionParameters.choosing(newTracks, it)
                            }
                        }
                }
            }
            override fun onCues(cueGroup: CueGroup) { cues = cueGroup }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) { seekable = exo.isCurrentMediaItemSeekable }
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
            tracks = Tracks.EMPTY
            cues = CueGroup.EMPTY_TIME_ZERO
        }
    }

    OnStopEffect { player?.pause() }

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

    fun commitScrub() {
        val target = scrub?.targetMs ?: return
        player?.seekTo(target)
        scrub = null
    }

    // The cursor lands by itself once the user stops pressing; each press restarts the wait.
    LaunchedEffect(scrub?.lastPressAtMs) {
        if (scrub == null) return@LaunchedEffect
        delay(SCRUB_COMMIT_DELAY_MS)
        commitScrub()
    }
    BackHandler(enabled = scrub != null) { scrub = null }

    fun stepScrub(direction: Int) {
        val exo = player ?: return
        if (durationMs <= 0) return
        // The bar says why nothing moves; a cursor on a file that cannot be sought would only jump back to the start.
        if (!seekable) {
            showControls()
            return
        }
        scrub = SeekScrubber.step(scrub, direction, exo.currentPosition, durationMs, System.currentTimeMillis())
        showControls()
    }

    fun openOptions() {
        scrub = null
        optionsPage = OptionsPage.Root
        optionsOpen = true
    }

    fun choose(kind: TrackKind, option: TrackOption?) {
        val exo = player ?: return
        exo.trackSelectionParameters = exo.trackSelectionParameters.choosing(tracks, kind, option)
        onEvent(PlayerEvent.RememberLanguage(kind, option?.language))
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // The panel took focus with it when it left the screen; the player surface needs it back for the remote.
    LaunchedEffect(optionsOpen) { if (!optionsOpen) focusRequester.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || state.error != null || optionsOpen) return@onKeyEvent false
                val exo = player
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.MediaPlayPause -> {
                        when {
                            state.isLive -> showControls()
                            scrub != null -> commitScrub()
                            else -> { exo?.let { if (it.isPlaying) it.pause() else it.play() }; showControls() }
                        }
                        true
                    }
                    Key.MediaPlay -> { exo?.play(); true }
                    Key.MediaPause -> { exo?.pause(); true }
                    Key.DirectionLeft, Key.MediaRewind -> if (!state.isLive && exo != null) { stepScrub(-1); true } else false
                    Key.DirectionRight, Key.MediaFastForward -> if (!state.isLive && exo != null) { stepScrub(+1); true } else false
                    Key.Menu -> { openOptions(); true }
                    Key.DirectionUp -> if (state.isLive) { onEvent(PlayerEvent.ZapNext); true } else { openOptions(); true }
                    Key.ChannelUp -> if (state.isLive) { onEvent(PlayerEvent.ZapNext); true } else false
                    Key.DirectionDown, Key.ChannelDown -> if (state.isLive) { onEvent(PlayerEvent.ZapPrevious); true } else false
                    else -> false
                }
            }
    ) {
        player?.let {
            ContentFrame(
                player = it,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                contentScale = when (aspect) {
                    AspectMode.FIT -> ContentScale.Fit
                    AspectMode.FILL -> ContentScale.Crop
                    AspectMode.STRETCH -> ContentScale.FillBounds
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        SubtitleOverlay(cues = cues.cues, style = state.settings.subtitleStyle, modifier = Modifier.fillMaxSize())

        when {
            state.error != null -> ErrorOverlay(state, onEvent, onExit)
            state.isLoading || (isBuffering && !isPlaying) -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState(label = stringResource(if (state.isLoading) R.string.player_loading else R.string.player_buffering))
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && state.error == null && !optionsOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            InfoOverlay(state, positionMs, durationMs, isPlaying, scrub, seekable)
        }

        AnimatedVisibility(visible = optionsOpen, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }, modifier = Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f - OPTIONS_PANEL_WIDTH_FRACTION))
                PlayerOptionsPanel(
                    page = optionsPage,
                    onPage = { optionsPage = it },
                    onClose = { optionsOpen = false },
                    audio = tracks.toOptions(TrackKind.AUDIO),
                    subtitles = tracks.toOptions(TrackKind.SUBTITLE),
                    onSelectAudio = { choose(TrackKind.AUDIO, it) },
                    onSelectSubtitle = { choose(TrackKind.SUBTITLE, it) },
                    style = state.settings.subtitleStyle,
                    onStyle = { onEvent(PlayerEvent.SetSubtitleStyle(it)) },
                    speed = speed,
                    onSpeed = {
                        speed = it
                        player?.setPlaybackSpeed(it)
                    },
                    aspect = aspect,
                    onAspect = { aspect = it },
                    modifier = Modifier.weight(OPTIONS_PANEL_WIDTH_FRACTION).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun InfoOverlay(state: PlayerUiState, positionMs: Long, durationMs: Long, isPlaying: Boolean, scrub: ScrubState?, seekable: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            if (state.isLive) {
                Text(
                    stringResource(R.string.player_live),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(TilfazColors.Danger).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(state.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
            if (!state.isLive && !isPlaying) Text("⏸", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
        if (state.isLive) {
            state.nowNext.now?.let {
                Text("${stringResource(R.string.player_now)}: ${it.title}", style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
            }
            state.nowNext.next?.let {
                Text(
                    "${stringResource(R.string.player_next)}: ${it.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TilfazColors.OnSurfaceMuted,
                    maxLines = 1
                )
            }
        } else if (durationMs > 0) {
            PlayerSeekBar(positionMs = positionMs, durationMs = durationMs, scrub = scrub, modifier = Modifier.fillMaxWidth(), seekable = seekable)
        }
        Text(
            stringResource(if (state.isLive) R.string.player_hint_live else R.string.player_hint_vod),
            style = MaterialTheme.typography.bodySmall,
            color = TilfazColors.OnSurfaceMuted
        )
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
        horizontalAlignment = Alignment.CenterHorizontally
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
            modifier = Modifier.padding(top = Dimens.SpaceS, bottom = Dimens.SpaceL)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            TilfazButton(onClick = {
                onEvent(PlayerEvent.Retry)
            }, modifier = Modifier.focusRequester(retryFocus)) { Text(stringResource(R.string.player_retry)) }
            if (state.canSwitchFormat) {
                OutlinedButton(onClick = { onEvent(PlayerEvent.SwitchFormat) }) {
                    Text(stringResource(if (state.alternativeFormat == StreamFormat.HLS) R.string.player_switch_to_hls else R.string.player_switch_to_ts))
                }
            }
            OutlinedButton(onClick = onExit) { Text(stringResource(R.string.player_back)) }
        }
        error.detail?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = TilfazColors.OnSurfaceMuted, modifier = Modifier.padding(top = Dimens.SpaceL))
        }
    }
}

private const val CONTROLS_TIMEOUT_MS = 5_000L

private const val OPTIONS_PANEL_WIDTH_FRACTION = 0.38f

// How long the seek cursor waits after the last press before the seek happens.
private const val SCRUB_COMMIT_DELAY_MS = 900L
