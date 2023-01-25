package com.blockchain.api.assetdiscovery.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class DynamicCurrency(
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
    val networkSymbol: String
        get() = symbol

    val displaySymbol: String
        get() = display ?: symbol
}

internal val assetTypeSerializers = SerializersModule {
    polymorphic(AssetType::class) {
        subclass(CoinAsset::class)
        subclass(Erc20Asset::class)
        subclass(CeloTokenAsset::class)
        subclass(FiatAsset::class)
        subclass(AssetInformationDto::class)
        default { UnsupportedAsset.serializer() }
    }
}

@Serializable
abstract class AssetType {
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

@Serializable
data class AssetInformationDto(
    @SerialName("currencyInfo")
    val assetInfo: DynamicCurrency?,
    @SerialName("description")
    val description: String?,
    @SerialName("whitepaper")
    val whitepaper: String?,
    @SerialName("website")
    val website: String?,
    @SerialName("language")
    val language: String?
) : AssetType()
