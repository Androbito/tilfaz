package com.androbito.tilfaz.feature.player

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.androbito.tilfaz.core.model.PlaybackSettings
import java.util.Locale

enum class TrackKind(val trackType: Int) {
    AUDIO(C.TRACK_TYPE_AUDIO),
    SUBTITLE(C.TRACK_TYPE_TEXT)
}

/** One audio or subtitle track the user can pick, described in words rather than codec flags. */
data class TrackOption(
    /** "groupIndex:trackIndex" in the player's current tracks. */
    val id: String,
    val kind: TrackKind,
    val label: String,
    val detail: String?,
    val language: String?,
    val isSelected: Boolean
)

fun Tracks.toOptions(kind: TrackKind): List<TrackOption> {
    val options = mutableListOf<TrackOption>()
    groups.forEachIndexed { groupIndex, group ->
        if (group.type != kind.trackType) return@forEachIndexed
        for (trackIndex in 0 until group.length) {
            // A track the device cannot play would be a dead end in the list.
            if (!group.isTrackSupported(trackIndex)) continue
            val format = group.getTrackFormat(trackIndex)
            val language = format.language?.takeIf { it.isNotBlank() && it != "und" }
            options += TrackOption(
                id = "$groupIndex:$trackIndex",
                kind = kind,
                label = language?.let(::languageName) ?: "Track ${options.size + 1}",
                detail = detailOf(kind, format.label, format.channelCount, format.sampleMimeType),
                language = language,
                isSelected = group.isTrackSelected(trackIndex)
            )
        }
    }
    return options
}

private fun languageName(code: String): String? =
    Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()).takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
        ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun detailOf(kind: TrackKind, label: String?, channels: Int, mimeType: String?): String? {
    val parts = when (kind) {
        TrackKind.AUDIO -> listOfNotNull(label?.takeIf { it.isNotBlank() }, channelLayout(channels), codecName(mimeType))
        TrackKind.SUBTITLE -> listOfNotNull(label?.takeIf { it.isNotBlank() })
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun channelLayout(channels: Int): String? = when (channels) {
    androidx.media3.common.Format.NO_VALUE, 0 -> null
    1 -> "Mono"
    2 -> "Stereo"
    6 -> "5.1"
    8 -> "7.1"
    else -> "$channels ch"
}

private fun codecName(mimeType: String?): String? = when (mimeType) {
    MimeTypes.AUDIO_AAC -> "AAC"
    MimeTypes.AUDIO_AC3 -> "AC3"
    MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3"
    MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_DTS_EXPRESS -> "DTS"
    MimeTypes.AUDIO_MPEG -> "MP3"
    MimeTypes.AUDIO_MPEG_L2 -> "MP2"
    MimeTypes.AUDIO_OPUS -> "Opus"
    MimeTypes.AUDIO_TRUEHD -> "TrueHD"
    MimeTypes.AUDIO_FLAC -> "FLAC"
    else -> null
}

/**
 * What the player should pick on its own: the preferred audio language if the stream has it, and subtitles in the
 * preferred language, or none when there is no preference (the user turns them on from the options).
 */
fun trackSelectionFor(settings: PlaybackSettings): TrackSelectionParameters {
    val builder = TrackSelectionParameters.DEFAULT.buildUpon()
    settings.audioLanguage?.let { builder.setPreferredAudioLanguage(it) }
    val subtitleLanguage = settings.subtitleLanguage
    if (subtitleLanguage == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
    } else {
        builder.setPreferredTextLanguage(subtitleLanguage).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
    }
    return builder.build()
}
