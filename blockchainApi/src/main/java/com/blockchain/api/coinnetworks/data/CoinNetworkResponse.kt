package com.blockchain.api.coinnetworks.data

import com.blockchain.domain.wallet.NetworkType
import com.blockchain.domain.wallet.STYLE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinNetworkResponse(
    @SerialName("networks")
    val networks: List<CoinNetworkDto>,
    @SerialName("types")
    val types: List<CoinTypeDto>
)

@Serializable
data class CoinNetworkDto(
    @SerialName("explorerUrl")
    val explorerUrl: String,
    @SerialName("nativeAsset")
    val currency: String,
    @SerialName("networkTicker")
    val network: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: NetworkType = NetworkType.NOT_SUPPORTED,
    @SerialName("identifiers")
    val identifiers: Identifiers,
    @SerialName("nodeUrls")
    val nodeUrls: List<String>,
    @SerialName("feeCurrencies")
    val feeCurrencies: List<String>,
    @SerialName("memos")
    val isMemoSupported: Boolean
)

@Serializable
data class Identifiers(
    @SerialName("chainId")
    val chainId: Int? = null
)

@Serializable
data class CoinTypeDto(
    @SerialName("type")
    val type: NetworkType = NetworkType.NOT_SUPPORTED,
    @SerialName("derivations")
    val derivations: List<DerivationDto>,
    @SerialName("style")
    val style: STYLE
)

@Serializable
data class DerivationDto(
    @SerialName("purpose")
    val purpose: Int,
    @SerialName("coinType")
    val coinType: Int,
    @SerialName("descriptor")
    val descriptor: Int
)
