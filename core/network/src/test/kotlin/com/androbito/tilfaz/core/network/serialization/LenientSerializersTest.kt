package com.androbito.tilfaz.core.network.serialization

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.network.dto.LiveStreamDto
import com.androbito.tilfaz.core.network.dto.SeriesInfoResponseDto
import com.androbito.tilfaz.core.network.dto.UserInfoDto
import com.androbito.tilfaz.core.network.dto.VodStreamDto
import kotlinx.serialization.json.Json
import org.junit.Test

class LenientSerializersTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun `numbers arrive as strings, numbers or empty and all parse`() {
        val asString = json.decodeFromString<LiveStreamDto>("""{"stream_id":"101","num":"3","tv_archive":"1"}""")
        val asNumber = json.decodeFromString<LiveStreamDto>("""{"stream_id":101,"num":3,"tv_archive":1}""")
        val empty = json.decodeFromString<LiveStreamDto>("""{"stream_id":"","num":null,"tv_archive":"null"}""")

        assertThat(asString.streamId).isEqualTo(101)
        assertThat(asNumber.streamId).isEqualTo(101)
        assertThat(asString.tvArchive).isTrue()
        assertThat(asNumber.tvArchive).isTrue()
        assertThat(empty.streamId).isNull()
        assertThat(empty.num).isNull()
        assertThat(empty.tvArchive).isNull()
    }

    @Test
    fun `ratings with decimal comma or as integers parse to doubles`() {
        val dto = json.decodeFromString<VodStreamDto>("""{"rating":"7,5","rating_5based":4,"category_id":12}""")
        assertThat(dto.rating).isEqualTo(7.5)
        assertThat(dto.rating5Based).isEqualTo(4.0)
        assertThat(dto.categoryId).isEqualTo("12")
    }

    @Test
    fun `allowed output formats accept array or comma separated string`() {
        val array = json.decodeFromString<UserInfoDto>("""{"allowed_output_formats":["m3u8","ts"],"exp_date":"1772000000","auth":1}""")
        val string = json.decodeFromString<UserInfoDto>("""{"allowed_output_formats":"m3u8, ts","exp_date":null,"auth":"0"}""")

        assertThat(array.allowedOutputFormats).containsExactly("m3u8", "ts")
        assertThat(string.allowedOutputFormats).containsExactly("m3u8", "ts")
        assertThat(array.expDateEpochSeconds).isEqualTo(1772000000L)
        assertThat(string.expDateEpochSeconds).isNull()
        assertThat(array.auth).isTrue()
        assertThat(string.auth).isEqualTo(false)
    }

    @Test
    fun `episodes keyed by season or as nested arrays both become a season map`() {
        val keyed = json.decodeFromString<SeriesInfoResponseDto>("""{"episodes":{"1":[{"id":"1"}],"2":[{"id":"2"},{"id":"3"}]}}""")
        val nested = json.decodeFromString<SeriesInfoResponseDto>("""{"episodes":[[{"id":"1"}],[{"id":"2"},{"id":"3"}]]}""")
        val missing = json.decodeFromString<SeriesInfoResponseDto>("""{"seasons":[]}""")

        assertThat(keyed.episodes.mapValues { it.value.size }).isEqualTo(mapOf("1" to 1, "2" to 2))
        assertThat(nested.episodes.mapValues { it.value.size }).isEqualTo(mapOf("1" to 1, "2" to 2))
        assertThat(missing.episodes).isEqualTo(emptyMap())
    }
}
