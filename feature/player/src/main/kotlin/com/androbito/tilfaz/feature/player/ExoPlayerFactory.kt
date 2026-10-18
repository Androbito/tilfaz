package com.androbito.tilfaz.feature.player

import android.app.ActivityManager
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import okhttp3.OkHttpClient

/**
 * Builds an ExoPlayer tuned for IPTV: OkHttp data source (same client, same user agent as the API),
 * buffer sizes from the user's profile, and extension renderers preferred so an FFmpeg module, if bundled,
 * handles the odd audio codecs providers ship (AC3, E-AC3, DTS, MP2).
 */
@UnstableApi
class ExoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    fun create(source: StreamSource, settings: PlaybackSettings): ExoPlayer {
        val buffer = settings.bufferProfile
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(buffer.minBufferMs, buffer.maxBufferMs, buffer.playbackBufferMs, buffer.playbackBufferMs * 2)
            // Time alone would let a high bitrate film outgrow the app's memory; the size cap wins over the time target.
            .setTargetBufferBytes(BufferLimits.targetBytes((context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass))
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        // Declared as film audio with focus handling: when something else takes the audio, the player pauses
        // instead of carrying on silently, and the system knows what this stream is.
        return build(source, loadControl, ownsAudio = true, trackSelection = trackSelectionFor(settings))
    }

    /**
     * The small live video of the Live screen: it has no audio track at all (so it can neither make a sound nor take audio
     * focus from the full screen player), decodes a small picture, and keeps a small buffer. It starts by itself and must be
     * released as soon as it is not shown, because the account allows a single connection.
     */
    fun createPreview(source: StreamSource): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(PREVIEW_MIN_BUFFER_MS, PREVIEW_MAX_BUFFER_MS, PREVIEW_START_BUFFER_MS, PREVIEW_START_BUFFER_MS * 2)
            .setTargetBufferBytes(PREVIEW_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        return build(source, loadControl, ownsAudio = false, trackSelection = previewTrackSelection, liveOffsetMs = PREVIEW_LIVE_OFFSET_MS)
    }

    /** What a preview of [source] plays: the same stream as full screen, but close to the live edge. */
    fun previewMediaItem(source: StreamSource): MediaItem = source.toMediaItem(PREVIEW_LIVE_OFFSET_MS)

    /** Turns a small preview player into the full screen one in place: sound, subtitles and the full picture. No restart. */
    fun upgradeToFull(player: ExoPlayer, settings: PlaybackSettings) {
        player.setAudioAttributes(movieAudio, true)
        player.trackSelectionParameters = trackSelectionFor(settings)
        player.setWakeMode(C.WAKE_MODE_NETWORK)
        player.playWhenReady = true
    }

    /** The other way: silent, small, and not holding the audio focus or the wake lock. */
    fun downgradeToPreview(player: ExoPlayer) {
        player.setAudioAttributes(movieAudio, false)
        player.trackSelectionParameters = previewTrackSelection
        player.setWakeMode(C.WAKE_MODE_NONE)
    }

    private val movieAudio = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()

    private val previewTrackSelection: TrackSelectionParameters = TrackSelectionParameters.DEFAULT.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .setMaxVideoSize(PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT)
        .build()

    private fun build(
        source: StreamSource,
        loadControl: DefaultLoadControl,
        ownsAudio: Boolean,
        trackSelection: TrackSelectionParameters,
        liveOffsetMs: Long = LIVE_TARGET_OFFSET_MS
    ): ExoPlayer {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(source.userAgent)
            .setDefaultRequestProperties(mapOf("Accept" to "*/*"))

        // Movies and episodes are progressive files; some Matroska ones need their seek index pointed out to be seekable.
        val mediaDataSourceFactory = if (source.format == null) {
            DataSource.Factory { MatroskaCuesDataSource(dataSourceFactory) }
        } else {
            dataSourceFactory
        }

        val renderers = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(mediaDataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(movieAudio, ownsAudio)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(if (ownsAudio) C.WAKE_MODE_NETWORK else C.WAKE_MODE_NONE)
            .build()
            .apply {
                trackSelectionParameters = trackSelection
                setMediaItem(source.toMediaItem(liveOffsetMs))
                playWhenReady = true
            }
    }

    private fun StreamSource.toMediaItem(liveOffsetMs: Long): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        when (format) {
            StreamFormat.HLS -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            StreamFormat.MPEG_TS -> builder.setMimeType(MimeTypes.VIDEO_MP2T)
            null -> Unit // progressive VOD: let the extractor sniff mp4/mkv/avi
        }
        if (isLive) {
            builder.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(liveOffsetMs).build())
        }
        return builder.build()
    }

    companion object {
        const val PREVIEW_MAX_WIDTH = 640
        const val PREVIEW_MAX_HEIGHT = 360
        private const val PREVIEW_MIN_BUFFER_MS = 1_500
        private const val PREVIEW_MAX_BUFFER_MS = 5_000
        private const val PREVIEW_START_BUFFER_MS = 500
        private const val PREVIEW_BUFFER_BYTES = 8 * 1024 * 1024
        private const val LIVE_TARGET_OFFSET_MS = 6_000L

        /** A preview does not need a safety margin against stalls, so it plays closer to the live edge and shows a picture sooner. */
        private const val PREVIEW_LIVE_OFFSET_MS = 2_000L
    }
}

/** Translates ExoPlayer error codes into the handful of situations the UI explains differently. */
fun PlaybackException.toPlayerError(): PlayerError {
    val kind = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        -> PlayerErrorKind.NOT_FOUND
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        -> PlayerErrorKind.NETWORK
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE
        -> PlayerErrorKind.SOURCE
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
        -> PlayerErrorKind.DECODER
        else -> PlayerErrorKind.UNKNOWN
    }
    return PlayerError(kind, errorCodeName)
}
