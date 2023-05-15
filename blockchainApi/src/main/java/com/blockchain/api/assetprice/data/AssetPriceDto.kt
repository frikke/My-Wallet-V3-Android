package com.blockchain.api.assetprice.data

import com.blockchain.api.assetprice.serialiser.AssetPriceDeserializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PriceSymbolDto(
    @SerialName("code")
    val code: String, // "UNI"/"CAD"
    @SerialName("symbol")
    val ticker: String, // "UNI"/"CAD"
    @SerialName("description")
    val name: String, // "Uniswap"/"Canadian Dollar"
    @SerialName("decimals")
    val precisionDp: Int, // 18/2
    @SerialName("fiat")
    val isFiat: Boolean // true/false
)

@Serializable
data class AvailableSymbolsDto(
    @SerialName("Base")
    val baseSymbols: Map<String, PriceSymbolDto>,
    @SerialName("Quote")
    val quoteSymbols: Map<String, PriceSymbolDto>
)

@Serializable(with = AssetPriceDeserializer::class)
internal data class AssetPriceDto(
    @SerialName("timestamp")
    val timestampSeconds: Long,
    @SerialName("price")
    val price: Double? = null,
    @SerialName("volume24h")
    val volume24h: Double? = null,
    @SerialName("marketCap")
    val marketCap: Double? = null
)

@Serializable
internal data class PriceRequestPairDto(
    @SerialName("base")
    val base: String,
    @SerialName("quote")
    val quote: String
)

internal typealias PriceResponseMapDto = Map<String, AssetPriceDto>
