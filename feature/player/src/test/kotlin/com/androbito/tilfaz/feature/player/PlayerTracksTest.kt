package com.androbito.tilfaz.feature.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.model.PlaybackSettings
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlayerTracksTest {
    @Before
    fun fixLocale() = Locale.setDefault(Locale.ENGLISH)

    private fun audio(language: String?, channels: Int = 2, mime: String = MimeTypes.AUDIO_AAC, label: String? = null) =
        Format.Builder().setSampleMimeType(mime).setLanguage(language).setChannelCount(channels).setLabel(label).build()

    private fun text(language: String?, label: String? = null) =
        Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_SUBRIP).setLanguage(language).setLabel(label).build()

    /** A group's type (audio or text) comes from the MIME type of its formats. */
    private fun group(vararg formats: Format, selected: Int? = null) = Tracks.Group(
        TrackGroup(*formats),
        false,
        IntArray(formats.size) { C.FORMAT_HANDLED },
        BooleanArray(formats.size) { it == selected }
    )

    private val tracks = Tracks(
        listOf(
            group(audio("ar", channels = 6, mime = MimeTypes.AUDIO_AC3), selected = 0),
            group(audio("en")),
            group(text("fr", label = "Français (SDH)")),
            group(text(null))
        )
    )

    @Test
    fun `audio tracks are named by language and say how they sound`() {
        val options = tracks.toOptions(TrackKind.AUDIO)

        assertThat(options.map { it.label }).isEqualTo(listOf("Arabic", "English"))
        assertThat(options[0].detail).isEqualTo("5.1 · AC3")
        assertThat(options[1].detail).isEqualTo("Stereo · AAC")
    }

    @Test
    fun `the playing track is marked and the others are not`() {
        val options = tracks.toOptions(TrackKind.AUDIO)

        assertThat(options[0].isSelected).isTrue()
        assertThat(options[1].isSelected).isFalse()
    }

    @Test
    fun `a track keeps the language code so it can be remembered`() {
        assertThat(tracks.toOptions(TrackKind.AUDIO)[0].language).isEqualTo("ar")
    }

    @Test
    fun `a provider label is kept next to the language`() {
        val subtitle = tracks.toOptions(TrackKind.SUBTITLE)[0]

        assertThat(subtitle.label).isEqualTo("French")
        assertThat(subtitle.detail).isEqualTo("Français (SDH)")
    }

    @Test
    fun `a track with no language is numbered instead of left blank`() {
        val unnamed = tracks.toOptions(TrackKind.SUBTITLE)[1]

        assertThat(unnamed.label).isEqualTo("Track 2")
        assertThat(unnamed.language).isNull()
    }

    @Test
    fun `audio and subtitles are listed separately`() {
        assertThat(tracks.toOptions(TrackKind.AUDIO).size).isEqualTo(2)
        assertThat(tracks.toOptions(TrackKind.SUBTITLE).size).isEqualTo(2)
    }

    @Test
    fun `a preferred audio language is asked of the player`() {
        val parameters = trackSelectionFor(PlaybackSettings(audioLanguage = "ar"))

        assertThat(parameters.preferredAudioLanguages).contains("ar")
    }

    @Test
    fun `without a subtitle preference subtitles stay off`() {
        val parameters = trackSelectionFor(PlaybackSettings(subtitleLanguage = null))

        assertThat(parameters.disabledTrackTypes).contains(C.TRACK_TYPE_TEXT)
    }

    @Test
    fun `a preferred subtitle language turns subtitles on in that language`() {
        val parameters = trackSelectionFor(PlaybackSettings(subtitleLanguage = "en"))

        assertThat(parameters.preferredTextLanguages).contains("en")
        assertThat(parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)).isFalse()
    }

    @Test
    fun `a stream label that only repeats the language is dropped from the detail`() {
        val repeated = Tracks(listOf(group(audio("ar", label = "Arabic"), selected = 0)))

        assertThat(repeated.toOptions(TrackKind.AUDIO)[0].detail).isEqualTo("Stereo · AAC")
    }

    @Test
    fun `choosing an audio track overrides the selection for that track only`() {
        val english = tracks.toOptions(TrackKind.AUDIO)[1]

        val chosen = TrackSelectionParameters.DEFAULT.choosing(tracks, english)

        val override = chosen.overrides.values.single()
        assertThat(override.mediaTrackGroup).isEqualTo(tracks.groups[1].mediaTrackGroup)
        assertThat(override.trackIndices).isEqualTo(listOf(0))
    }

    @Test
    fun `choosing a subtitle turns the text renderer on`() {
        val french = tracks.toOptions(TrackKind.SUBTITLE)[0]
        val off = TrackSelectionParameters.DEFAULT.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()

        val chosen = off.choosing(tracks, french)

        assertThat(chosen.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)).isFalse()
        assertThat(chosen.overrides.values.single().mediaTrackGroup).isEqualTo(tracks.groups[2].mediaTrackGroup)
    }

    @Test
    fun `choosing no subtitle disables text and drops the subtitle override`() {
        val french = tracks.toOptions(TrackKind.SUBTITLE)[0]
        val english = tracks.toOptions(TrackKind.AUDIO)[1]
        val withBoth = TrackSelectionParameters.DEFAULT.choosing(tracks, english).choosing(tracks, french)

        val chosen = withBoth.choosing(tracks, TrackKind.SUBTITLE, null)

        assertThat(chosen.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)).isTrue()
        assertThat(chosen.overrides.values.map { it.mediaTrackGroup }).isEqualTo(listOf(tracks.groups[1].mediaTrackGroup))
    }
}
