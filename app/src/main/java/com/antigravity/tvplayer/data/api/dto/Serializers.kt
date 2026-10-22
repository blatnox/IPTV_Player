package com.antigravity.tvplayer.data.api.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

object StringOrIntSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.content
            } else {
                element.toString()
            }
        } else {
            decoder.decodeString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

object IntOrStringSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntOrString", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.intOrNull ?: element.content.toIntOrNull() ?: 0
            } else {
                0
            }
        } else {
            decoder.decodeInt()
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}
