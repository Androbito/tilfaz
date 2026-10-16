package com.androbito.tilfaz.core.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/*
 * Xtream Codes panels are PHP apps with no fixed schema: the same field arrives as 12, "12", "", null or "null"
 * depending on the provider. These serializers accept all of that and only ever produce a clean nullable value,
 * so DTO parsing never fails on a type mismatch and the mapper decides what a missing value means.
 */

private fun Decoder.jsonElementOrNull(): JsonElement? {
    val element = (this as? JsonDecoder)?.decodeJsonElement() ?: return null
    return element.takeUnless { it is JsonNull }
}

private fun JsonElement?.primitiveText(): String? = when (this) {
    null, is JsonNull -> null
    is JsonPrimitive -> contentOrNull?.trim()?.takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) }
    is JsonArray -> null
    is JsonObject -> null
}

object LenientStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String? = decoder.jsonElementOrNull().primitiveText()
    override fun serialize(encoder: Encoder, value: String?) = if (value == null) encoder.encodeNull() else encoder.encodeString(value)
}

object LenientIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Int? = decoder.jsonElementOrNull().primitiveText()?.toDoubleOrNull()?.toInt()
    override fun serialize(encoder: Encoder, value: Int?) = if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
}

object LenientLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): Long? = decoder.jsonElementOrNull().primitiveText()?.toDoubleOrNull()?.toLong()
    override fun serialize(encoder: Encoder, value: Long?) = if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
}

object LenientDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)
    override fun deserialize(decoder: Decoder): Double? = decoder.jsonElementOrNull().primitiveText()?.replace(',', '.')?.toDoubleOrNull()
    override fun serialize(encoder: Encoder, value: Double?) = if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
}

/** Accepts true/false, 1/0, "1"/"0", "true"/"false", "yes"/"no". */
object LenientBooleanSerializer : KSerializer<Boolean?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientBoolean", PrimitiveKind.BOOLEAN)
    override fun deserialize(decoder: Decoder): Boolean? = when (decoder.jsonElementOrNull().primitiveText()?.lowercase()) {
        null -> null
        "1", "true", "yes", "on" -> true
        "0", "false", "no", "off" -> false
        else -> null
    }
    override fun serialize(encoder: Encoder, value: Boolean?) = if (value == null) encoder.encodeNull() else encoder.encodeBoolean(value)
}

/** A list of strings that may arrive as an array, a single string, a comma separated string, or nothing. */
object LenientStringListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LenientStringList")
    override fun deserialize(decoder: Decoder): List<String> = when (val element = decoder.jsonElementOrNull()) {
        null -> emptyList()
        is JsonArray -> element.mapNotNull { it.primitiveText() }
        is JsonPrimitive -> element.primitiveText()?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        is JsonObject -> element.values.mapNotNull { it.primitiveText() }
    }
    override fun serialize(encoder: Encoder, value: List<String>) = throw UnsupportedOperationException("read-only")
}

/** `episodes` in get_series_info is usually {"1": [...], "2": [...]} but some panels send [[...], [...]]. */
object EpisodesBySeasonSerializer : KSerializer<Map<String, List<JsonElement>>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EpisodesBySeason")
    override fun deserialize(decoder: Decoder): Map<String, List<JsonElement>> = when (val element = decoder.jsonElementOrNull()) {
        null -> emptyMap()
        is JsonObject -> element.mapValues { (_, v) -> (v as? JsonArray)?.toList().orEmpty() }
        is JsonArray -> element.mapIndexed { index, v -> (index + 1).toString() to (v as? JsonArray)?.toList().orEmpty() }.toMap()
        is JsonPrimitive -> emptyMap()
    }
    override fun serialize(encoder: Encoder, value: Map<String, List<JsonElement>>) = throw UnsupportedOperationException("read-only")
}

internal fun JsonElement.textOrNull(): String? = (this as? JsonPrimitive)?.jsonPrimitive?.primitiveText()
