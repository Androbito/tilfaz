package com.androbito.tilfaz.core.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.network.dto.VodStreamDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test

class JsonArrayStreamTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun body(text: String) = text.toResponseBody("application/json".toMediaType())

    @Test
    fun `the items of an array come out one at a time with the panel's quirks understood`() {
        val items = json.streamArray(
            body("""[{"stream_id":"1","name":"One","extra":true},{"stream_id":2,"name":"Two"}]"""),
            VodStreamDto.serializer()
        ).toList()

        assertThat(items.map { it.streamId }).containsExactly(1, 2)
        assertThat(items.map { it.name }).containsExactly("One", "Two")
    }

    @Test
    fun `an empty array is an empty catalog`() {
        assertThat(json.streamArray(body("[]"), VodStreamDto.serializer()).toList()).isEmpty()
    }

    @Test
    fun `items are read as they are needed, so a long catalog is never held whole`() {
        // The first item is fine and the rest is broken: taking one must not even look at the rest.
        val first = json.streamArray(body("""[{"stream_id":1,"name":"One"},{"stream_id": !!! broken"""), VodStreamDto.serializer()).first()

        assertThat(first.streamId).isEqualTo(1)
    }

    @Test
    fun `a broken array fails loudly instead of ending quietly with half a catalog`() {
        val broken = json.streamArray(body("""[{"stream_id":1,"name":"One"},{"stream_id": !!! broken"""), VodStreamDto.serializer())

        assertThrows(SerializationException::class.java) { broken.toList() }
    }

    @Test
    fun `an error object instead of an array fails loudly`() {
        // It is refused as soon as the stream is opened, before any item is looked at.
        assertThrows(SerializationException::class.java) {
            json.streamArray(body("""{"user_info":{"auth":0}}"""), VodStreamDto.serializer()).toList()
        }
    }
}
