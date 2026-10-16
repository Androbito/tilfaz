package com.androbito.tilfaz.feature.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Builds an ExoPlayer tuned for IPTV: OkHttp data source (same client, same user agent as the API),
 * buffer sizes from the user's profile, and extension renderers preferred so an FFmpeg module, if bundled,
 * handles the odd audio codecs providers ship (AC3, E-AC3, DTS, MP2).
 */
@UnstableApi
class ExoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    fun create(source: StreamSource, settings: PlaybackSettings): ExoPlayer {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(source.userAgent)
            .setDefaultRequestProperties(mapOf("Accept" to "*/*"))

        val buffer = settings.bufferProfile
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(buffer.minBufferMs, buffer.maxBufferMs, buffer.playbackBufferMs, buffer.playbackBufferMs * 2)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderers = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                setMediaItem(source.toMediaItem())
                playWhenReady = true
            }
    }

    private fun StreamSource.toMediaItem(): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        when (format) {
            StreamFormat.HLS -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            StreamFormat.MPEG_TS -> builder.setMimeType(MimeTypes.VIDEO_MP2T)
            null -> Unit // progressive VOD: let the extractor sniff mp4/mkv/avi
        }
        if (isLive) {
            builder.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(LIVE_TARGET_OFFSET_MS).build())
        }
        return builder.build()
    }

    private companion object {
        const val LIVE_TARGET_OFFSET_MS = 6_000L
    }
}

/** Translates ExoPlayer error codes into the handful of situations the UI explains differently. */
fun PlaybackException.toPlayerError(): PlayerError {
    val kind = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        -> PlayerErrorKind.NOT_FOUND
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        -> PlayerErrorKind.NETWORK
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        -> PlayerErrorKind.SOURCE
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        -> PlayerErrorKind.DECODER
        else -> PlayerErrorKind.UNKNOWN
    }
    return PlayerError(kind, errorCodeName)
}
