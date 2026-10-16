package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Test

class StreamUrlBuilderTest {
    private val settings = PlaybackSettings()

    @Test
    fun `live channel uses the configured format`() {
        val ts = StreamUrlBuilder.build(Fixtures.account, Playable.Live(Fixtures.alJazeera), settings)
        assertThat(ts.url).isEqualTo("http://tv.example.com:8080/live/ayoub/s3cret/101.ts")
        assertThat(ts.isLive).isTrue()

        val hls = StreamUrlBuilder.build(Fixtures.account, Playable.Live(Fixtures.alJazeera), settings.copy(liveFormat = StreamFormat.HLS))
        assertThat(hls.url).isEqualTo("http://tv.example.com:8080/live/ayoub/s3cret/101.m3u8")
        assertThat(hls.format).isEqualTo(StreamFormat.HLS)
    }

    @Test
    fun `movie keeps the provider container extension`() {
        val source = StreamUrlBuilder.build(Fixtures.account, Playable.Film(Fixtures.movie), settings)
        assertThat(source.url).isEqualTo("http://tv.example.com:8080/movie/ayoub/s3cret/501.mkv")
        assertThat(source.isLive).isFalse()
    }

    @Test
    fun `movie without extension falls back to mp4`() {
        val source = StreamUrlBuilder.build(Fixtures.account, Playable.Film(Fixtures.movie.copy(containerExtension = "")), settings)
        assertThat(source.url).isEqualTo("http://tv.example.com:8080/movie/ayoub/s3cret/501.mp4")
    }

    @Test
    fun `episode uses the series path and episode id`() {
        val source = StreamUrlBuilder.build(Fixtures.account, Playable.SeriesEpisode(Fixtures.episode1, "The Torment"), settings)
        assertThat(source.url).isEqualTo("http://tv.example.com:8080/series/ayoub/s3cret/90101.mp4")
        assertThat(source.playable.title).isEqualTo("The Torment S01E01")
    }

    @Test
    fun `credentials with special characters are percent encoded`() {
        val account = Account("http://tv.example.com", "a b@c", "p/w#1")
        val source = StreamUrlBuilder.build(account, Playable.Live(Fixtures.alJazeera), settings)
        assertThat(source.url).isEqualTo("http://tv.example.com/live/a%20b%40c/p%2Fw%231/101.ts")
    }

    @Test
    fun `user agent travels with the source`() {
        val source = StreamUrlBuilder.build(Fixtures.account, Playable.Live(Fixtures.alJazeera), settings.copy(userAgent = "VLC/3.0.20"))
        assertThat(source.userAgent).isEqualTo("VLC/3.0.20")
    }
}
