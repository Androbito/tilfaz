package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource

/**
 * Builds the playable URL for an Xtream Codes stream.
 *
 * Live:    {server}/live/{user}/{pass}/{streamId}.{ts|m3u8}
 * Movie:   {server}/movie/{user}/{pass}/{streamId}.{containerExtension}
 * Episode: {server}/series/{user}/{pass}/{episodeId}.{containerExtension}
 *
 * The live extension comes from the user's playback settings, so switching between MPEG-TS and HLS
 * is a settings change, not a provider change (this is the knob that fixes most "reading error" cases).
 */
object StreamUrlBuilder {

    fun build(account: Account, playable: Playable, settings: PlaybackSettings): StreamSource {
        val root = account.serverUrl.trimEnd('/')
        val credentials = "${encode(account.username)}/${encode(account.password)}"
        val (path, format) = when (playable) {
            is Playable.Live -> "live/$credentials/${playable.channel.id}.${settings.liveFormat.extension}" to settings.liveFormat
            is Playable.Film -> {
                val ext = playable.movie.containerExtension.ifBlank { DEFAULT_VOD_EXTENSION }
                "movie/$credentials/${playable.movie.id}.$ext" to StreamFormat.fromExtension(ext)
            }
            is Playable.SeriesEpisode -> {
                val ext = playable.episode.containerExtension.ifBlank { DEFAULT_VOD_EXTENSION }
                "series/$credentials/${encode(playable.episode.id)}.$ext" to StreamFormat.fromExtension(ext)
            }
        }
        return StreamSource(
            url = "$root/$path",
            playable = playable,
            isLive = playable is Playable.Live,
            userAgent = settings.userAgent,
            format = format,
        )
    }

    /** Percent-encodes a path segment without turning it into a query string (Xtream puts credentials in the path). */
    private fun encode(segment: String): String = buildString {
        for (byte in segment.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt().toChar()
            if (c.isLetterOrDigit() && byte >= 0 || c in UNRESERVED) append(c) else append('%').append("%02X".format(byte.toInt() and 0xFF))
        }
    }

    private const val UNRESERVED = "-._~"
    private const val DEFAULT_VOD_EXTENSION = "mp4"
}
