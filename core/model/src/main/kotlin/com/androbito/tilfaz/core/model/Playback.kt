package com.androbito.tilfaz.core.model

enum class StreamFormat(val extension: String) {
    MPEG_TS("ts"),
    HLS("m3u8"),
    ;

    companion object {
        fun fromExtension(value: String?): StreamFormat? = entries.firstOrNull { it.extension.equals(value, ignoreCase = true) }
    }
}

/** Something the player can play. Each variant carries exactly what the URL builder and the UI need. */
sealed interface Playable {
    val key: String
    val title: String

    data class Live(val channel: LiveChannel) : Playable {
        override val key: String get() = "live:${channel.id}"
        override val title: String get() = channel.name
    }

    data class Film(val movie: Movie) : Playable {
        override val key: String get() = "vod:${movie.id}"
        override val title: String get() = movie.name
    }

    data class SeriesEpisode(val episode: Episode, val seriesName: String) : Playable {
        override val key: String get() = "series:${episode.seriesId}:${episode.id}"
        override val title: String get() = "$seriesName ${episode.code}"
    }
}

data class StreamSource(
    val url: String,
    val playable: Playable,
    val isLive: Boolean,
    val userAgent: String,
    val format: StreamFormat?,
)

data class PlaybackSettings(
    val liveFormat: StreamFormat = StreamFormat.MPEG_TS,
    val userAgent: String = DEFAULT_USER_AGENT,
    val bufferProfile: BufferProfile = BufferProfile.BALANCED,
    val resumePlayback: Boolean = true,
) {
    companion object {
        const val DEFAULT_USER_AGENT = "TilfazTV/1.0 (Android TV)"
    }
}

enum class BufferProfile(val minBufferMs: Int, val maxBufferMs: Int, val playbackBufferMs: Int) {
    LOW_LATENCY(minBufferMs = 2_500, maxBufferMs = 15_000, playbackBufferMs = 1_000),
    BALANCED(minBufferMs = 15_000, maxBufferMs = 50_000, playbackBufferMs = 2_500),
    STABLE(minBufferMs = 30_000, maxBufferMs = 90_000, playbackBufferMs = 5_000),
}

data class WatchProgress(
    val playableKey: String,
    val contentType: ContentType,
    val title: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: java.time.Instant,
    /** Opaque reference to reopen the item: movie id, or "seriesId:episodeId". */
    val reference: String,
) {
    val fraction: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val isFinished: Boolean get() = fraction >= FINISHED_THRESHOLD

    companion object {
        const val FINISHED_THRESHOLD = 0.95f
    }
}
