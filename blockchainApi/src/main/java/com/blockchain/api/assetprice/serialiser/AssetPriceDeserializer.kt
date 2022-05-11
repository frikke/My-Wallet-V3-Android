package com.blockchain.api.assetprice.serialiser

import com.blockchain.api.assetprice.data.AssetPriceDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// The prices API actually returns `Map<String, AssetPriceDto?>`
// ie it can map a pair to null, to represent that a price is not available.
// Unfortunately the default retrofit/object factory fails with null elements
// in a raw map, so we need to have a custom deserializer to handle that situation
// hence:
internal class AssetPriceDeserializer : KSerializer<AssetPriceDto> {

    override fun deserialize(decoder: Decoder): AssetPriceDto {
        var timestamp = System.currentTimeMillis()
        var price: Double? = null
        var volume24h: Double? = null
        var marketCap: Double? = null

        if (decoder.decodeNotNullMark()) {
            decoder.beginStructure(descriptor).run {
                loop@ while (true) {
                    when (val i = decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break@loop
                        ELEMENT_IDX_TIMESTAMP -> timestamp = decodeLongElement(descriptor, i)
                        ELEMENT_IDX_PRICE ->
                            price =
                                if (decoder.decodeNotNullMark()) {
                                    decodeDoubleElement(descriptor, i)
                                } else {
                                    null
                                }
                        ELEMENT_IDX_VOLUME ->
                            volume24h =
                                if (decoder.decodeNotNullMark()) {
                                    decodeDoubleElement(descriptor, i)
                                } else {
                                    null
                                }
                        ELEMENT_MARKET_CAP ->
                            marketCap =
                                if (decoder.decodeNotNullMark()) {
                                    decodeDoubleElement(descriptor, i)
                                } else {
                                    null
                                }
                        else -> throw SerializationException("Unknown index $i")
                    }
                }
                endStructure(descriptor)
            }
        }
        return AssetPriceDto(timestamp, price, volume24h, marketCap)
    }

    override fun serialize(encoder: Encoder, value: AssetPriceDto) {
        // Do nothing - we don't use this
    }

    override val descriptor = buildClassSerialDescriptor("AssetPriceDto") {
        element<Long>("timestamp", isOptional = false)
        element<Double>("price", isOptional = true)
        element<Double>("volume24h", isOptional = true)
        element<Double>("marketCap", isOptional = true)
    }

    companion object {
        private const val ELEMENT_IDX_TIMESTAMP = 0
        private const val ELEMENT_IDX_PRICE = 1
        private const val ELEMENT_IDX_VOLUME = 2
        private const val ELEMENT_MARKET_CAP = 3
    }
}
