package com.blockchain.api.assetdiscovery.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
internal data class DynamicCurrency(
    @SerialName("symbol")
    private val symbol: String, // "ADA"
    @SerialName("displaySymbol")
    private val display: String? = null, // "ADA"
    @SerialName("name")
    val assetName: String,
    @SerialName("type")
    val coinType: AssetType,
    @SerialName("precision")
    val precision: Int,
    @SerialName("products")
    val products: List<String>
) {
    @Transient
    val networkSymbol: String = symbol
    @Transient
    val displaySymbol: String = display ?: symbol
}

internal val assetTypeSerializers = SerializersModule {
    polymorphic(AssetType::class) {
        subclass(CoinAsset::class)
        subclass(Erc20Asset::class)
        subclass(CeloTokenAsset::class)
        subclass(FiatAsset::class)
        default { UnsupportedAsset.serializer() }
    }
}

@Serializable
internal abstract class AssetType {
    @SerialName("logoPngUrl")
    val logoUrl: String? = null
    @SerialName("websiteUrl")
    val websiteUrl: String? = null
}

@Serializable
@SerialName("COIN")
internal data class CoinAsset(
    @SerialName("minimumOnChainConfirmations")
    val minConfirmations: Int
) : AssetType()

@Serializable
@SerialName("ERC20")
internal data class Erc20Asset(
    @SerialName("parentChain")
    val parentChain: String,
    @SerialName("erc20Address")
    val chainIdentifier: String
) : AssetType()

@Serializable
@SerialName("CELO_TOKEN")
internal data class CeloTokenAsset(
    @SerialName("parentChain")
    val parentChain: String,
    @SerialName("erc20Address")
    val chainIdentifier: String
) : AssetType()

@Serializable
@SerialName("FIAT")
internal data class FiatAsset(
    private val unused: String? = null
) : AssetType()

@Serializable
internal data class UnsupportedAsset(
    private val unused: String? = null
) : AssetType()

@Serializable
internal data class DynamicCurrencyList(
    @SerialName("currencies")
    val currencies: List<DynamicCurrency>
)
