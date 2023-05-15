package com.blockchain.nabu.metadata

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

object BlockchainAccountMetadataSerializer : KSerializer<BlockchainAccountCredentialsMetadata> {
    override fun deserialize(decoder: Decoder): BlockchainAccountCredentialsMetadata {
        decoder.decodeStructure(
            descriptor
        ) {
            var userId = ""
            var lifetimeToken = ""
            var exchangeUserId: String? = null
            var exchangeLifetimeToken: String? = null
            var corrupted = false
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> userId = decodeStringElement(descriptor, 0)
                    1 -> lifetimeToken = decodeStringElement(descriptor, 1)
                    2 -> exchangeUserId = decodeNullableSerializableElement(
                        descriptor,
                        2,
                        String.serializer() as DeserializationStrategy<String?>
                    )
                    3 -> exchangeLifetimeToken = decodeNullableSerializableElement(
                        descriptor,
                        3,
                        String.serializer() as DeserializationStrategy<String?>
                    )
                    4 -> corrupted = decodeBooleanElement(descriptor, 4)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            return BlockchainAccountCredentialsMetadata(
                userId = userId,
                lifetimeToken = lifetimeToken,
                exchangeUserId = exchangeUserId,
                exchangeLifetimeToken = exchangeLifetimeToken,
                isCorrupted = corrupted
            )
        }
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("CredentialMetadata") {
            element<String>(METADATA_USER_ID)
            element<String>(METADATA_LIFETIME_TOKEN)
            element<String>(METADATA_EXCHANGE_USER_ID)
            element<String>(METADATA_EXCHANGE_LIFETIME_TOKEN)
            element<String>(CORRUPTED_KEY)
        }

    override fun serialize(encoder: Encoder, value: BlockchainAccountCredentialsMetadata) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.userId.orEmpty())
            encodeStringElement(descriptor, 1, value.lifetimeToken.orEmpty())
            value.exchangeUserId?.let {
                encodeStringElement(descriptor, 2, it)
            }
            value.exchangeLifetimeToken?.let {
                encodeStringElement(descriptor, 3, it)
            }
        }
    }
}

object RootBlockchainAccountCredentialsMetadataSerializer :
    JsonTransformingSerializer<BlockchainAccountCredentialsMetadata>(BlockchainAccountMetadataSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return buildJsonObject {
            element.jsonObject.forEach { entry ->
                put(entry.key.normalisedForBlockchainAccountMetadata(), entry.value)
            }

            put(
                CORRUPTED_KEY,
                JsonPrimitive(
                    element.jsonObject.any {
                        it.key == CORRUPTED_USER_ID || it.key == CORRUPTED_LIFETIME_TOKEN ||
                            it.key == CORRUPTED_EXCHANGE_USER_ID || it.key == CORRUPTED_EXCHANGE_LIFETIME_TOKEN
                    }
                )
            )
        }
    }
}

private fun String.normalisedForBlockchainAccountMetadata(): String =
    when (this) {
        CORRUPTED_USER_ID,
        METADATA_USER_ID -> METADATA_USER_ID
        CORRUPTED_LIFETIME_TOKEN,
        METADATA_LIFETIME_TOKEN -> METADATA_LIFETIME_TOKEN
        CORRUPTED_EXCHANGE_USER_ID,
        METADATA_EXCHANGE_USER_ID -> METADATA_EXCHANGE_USER_ID
        CORRUPTED_EXCHANGE_LIFETIME_TOKEN,
        METADATA_EXCHANGE_LIFETIME_TOKEN -> METADATA_EXCHANGE_LIFETIME_TOKEN
        else -> this
    }

private const val CORRUPTED_KEY = "CORRUPTED_KEY"

private const val CORRUPTED_USER_ID = "userId"
private const val CORRUPTED_LIFETIME_TOKEN = "lifetimeToken"
private const val CORRUPTED_EXCHANGE_USER_ID = "exchangeUserId"
private const val CORRUPTED_EXCHANGE_LIFETIME_TOKEN = "exchangeLifetimeToken"

private const val METADATA_USER_ID = "nabu_user_id"
private const val METADATA_LIFETIME_TOKEN = "nabu_lifetime_token"
private const val METADATA_EXCHANGE_USER_ID = "exchange_user_id"
private const val METADATA_EXCHANGE_LIFETIME_TOKEN = "exchange_lifetime_token"
