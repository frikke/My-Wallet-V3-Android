package com.blockchain.nabu.metadata

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

object NabuLegacyAccountSerializer : KSerializer<NabuLegacyCredentialsMetadata> {

    override fun deserialize(decoder: Decoder): NabuLegacyCredentialsMetadata {
        decoder.decodeStructure(descriptor) {
            var userId = ""
            var lifetimeToken = ""
            var corrupted = false
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> userId = decodeStringElement(descriptor, 0)
                    1 -> lifetimeToken = decodeStringElement(descriptor, 1)
                    2 -> corrupted = decodeBooleanElement(descriptor, 2)
                    DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            return NabuLegacyCredentialsMetadata(
                userId,
                lifetimeToken,
                corrupted
            )
        }
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("CredentialMetadata") {
            element<String>(METADATA_USER_ID_TOKEN_TAG)
            element<String>(METADATA_LIFETIME_TOKEN_TAG)
            element<String>(CORRUPTED_KEY)
        }

    override fun serialize(encoder: Encoder, value: NabuLegacyCredentialsMetadata) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.userId)
            encodeStringElement(descriptor, 1, value.lifetimeToken)
        }
    }
}

object RootNabuLegacyAccountSerializer :
    JsonTransformingSerializer<NabuLegacyCredentialsMetadata>(NabuLegacyAccountSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return buildJsonObject {
            element.jsonObject.forEach { entry ->
                put(entry.key.normalisedForLegacyNabuMetadata(), entry.value)
            }
            put(
                CORRUPTED_KEY,
                JsonPrimitive(
                    element.jsonObject.any {
                        it.key == CORRUPTED_LIFETIME_TOKEN || it.key == CORRUPTED_USER_ID
                    }
                )
            )
        }
    }
}

private fun String.normalisedForLegacyNabuMetadata(): String {
    return when (this) {
        CORRUPTED_LIFETIME_TOKEN,
        METADATA_LIFETIME_TOKEN_TAG -> METADATA_LIFETIME_TOKEN_TAG
        CORRUPTED_USER_ID,
        METADATA_USER_ID_TOKEN_TAG -> METADATA_USER_ID_TOKEN_TAG
        else -> this
    }
}

private const val CORRUPTED_USER_ID = "userId"
private const val CORRUPTED_LIFETIME_TOKEN = "lifetimeToken"

private const val METADATA_LIFETIME_TOKEN_TAG = "lifetime_token"
private const val METADATA_USER_ID_TOKEN_TAG = "user_id"

private const val CORRUPTED_KEY = "corrupted"
