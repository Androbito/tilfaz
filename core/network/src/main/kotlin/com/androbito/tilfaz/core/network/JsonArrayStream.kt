package com.androbito.tilfaz.core.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import okhttp3.ResponseBody

/**
 * The items of a JSON array in [body], decoded one at a time as the sequence is consumed. A provider's movie list is tens of
 * megabytes of JSON: read whole it would fill the memory of a TV, read like this only the item being saved is alive.
 * The caller closes [body] once it is done with the sequence.
 */
@OptIn(ExperimentalSerializationApi::class)
fun <T> Json.streamArray(body: ResponseBody, serializer: DeserializationStrategy<T>): Sequence<T> =
    decodeToSequence(body.byteStream(), serializer, DecodeSequenceMode.ARRAY_WRAPPED)
