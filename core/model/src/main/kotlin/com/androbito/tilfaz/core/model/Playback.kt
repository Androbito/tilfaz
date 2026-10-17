package com.androbito.tilfaz.core.model

enum class StreamFormat(val extension: String) {
    MPEG_TS("ts"),
    HLS("m3u8")
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
    val format: StreamFormat?
)

data class PlaybackSettings(
    val liveFormat: StreamFormat = StreamFormat.MPEG_TS,
    val userAgent: String = DEFAULT_USER_AGENT,
    val bufferProfile: BufferProfile = BufferProfile.BALANCED,
    val resumePlayback: Boolean = true,
    /** ISO 639 code ("ar", "en"…) preferred for audio, or null to keep the stream's default. */
    val audioLanguage: String? = null,
    /** ISO 639 code preferred for subtitles, or null for none unless the user turns them on. */
    val subtitleLanguage: String? = null,
    val subtitleStyle: SubtitleStyle = SubtitleStyle()
) {
    companion object {
        const val DEFAULT_USER_AGENT = "TilfazTV/1.0 (Android TV)"
    }
}

data class SubtitleStyle(
    val size: SubtitleSize = SubtitleSize.MEDIUM,
    val textColor: SubtitleColor = SubtitleColor.WHITE,
    val background: SubtitleBackground = SubtitleBackground.TRANSLUCENT
)

/** [fractionOfVideoHeight] is the text height as a share of the video height, so it scales with the screen. */
enum class SubtitleSize(val fractionOfVideoHeight: Float) {
    SMALL(0.04f),
    MEDIUM(0.0533f),
    LARGE(0.07f),
    EXTRA_LARGE(0.09f)
}

enum class SubtitleColor(val argb: Int) {
    WHITE(0xFFFFFFFF.toInt()),
    YELLOW(0xFFFFE066.toInt()),
    CYAN(0xFF7DE3FF.toInt()),
    GREEN(0xFF8CF29A.toInt())
}

enum class SubtitleBackground(val argb: Int) {
    NONE(0x00000000),
    TRANSLUCENT(0x99000000.toInt()),
    BLACK(0xFF000000.toInt()),
    GRAY(0xCC4A4A4A.toInt()),
    BLUE(0xCC10306B.toInt())
}

enum class BufferProfile(val minBufferMs: Int, val maxBufferMs: Int, val playbackBufferMs: Int) {
    LOW_LATENCY(minBufferMs = 2_500, maxBufferMs = 15_000, playbackBufferMs = 1_000),
    BALANCED(minBufferMs = 15_000, maxBufferMs = 50_000, playbackBufferMs = 2_500),
    STABLE(minBufferMs = 30_000, maxBufferMs = 90_000, playbackBufferMs = 5_000)
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
    val reference: String
) {
    val fraction: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val isFinished: Boolean get() = fraction >= FINISHED_THRESHOLD

    companion object {
        const val FINISHED_THRESHOLD = 0.95f
    }
}
