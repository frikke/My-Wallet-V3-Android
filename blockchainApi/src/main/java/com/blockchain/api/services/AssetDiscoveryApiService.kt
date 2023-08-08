package com.blockchain.api.services

import com.blockchain.api.assetdiscovery.AssetDiscoveryApiInterface
import com.blockchain.api.assetdiscovery.data.AssetInformationDto
import com.blockchain.api.assetdiscovery.data.CeloTokenAsset
import com.blockchain.api.assetdiscovery.data.CoinAsset
import com.blockchain.api.assetdiscovery.data.DynamicCurrency
import com.blockchain.api.assetdiscovery.data.Erc20Asset
import com.blockchain.api.assetdiscovery.data.FiatAsset
import com.blockchain.api.assetdiscovery.data.UnsupportedAsset
import com.blockchain.api.coinnetworks.CoinNetworkApiInterface
import com.blockchain.api.coinnetworks.data.CoinNetworkDto
import com.blockchain.api.coinnetworks.data.CoinTypeDto
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.NetworkType
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DynamicAssetProducts {
    PrivateKey,
    MercuryDeposits, // Can create an address and deposit in the exchange
    MercuryWithdrawals, // Can withdraw funds from the exchange
    ServicePrice, // Service price has prices for it
    HWS, // HotWalletService supports it
    CustodialWalletBalance, // Can have a custodial/simplebuy balance of this currency
    InterestBalance, // Can have an interest balance
    EarnCC1W, // Can have an active rewards
    Staking, // Can have an interest balance
    DynamicSelfCustody
}

@Serializable
data class DynamicAsset(
    @SerialName("assetName")
    val assetName: String,
    @SerialName("networkTicker")
    val networkTicker: String,
    @SerialName("displayTicker")
    val displayTicker: String,
    @SerialName("isFiat")
    val isFiat: Boolean,
    @SerialName("precision")
    val precision: Int,
    @SerialName("products")
    val products: Set<DynamicAssetProducts>,
    @SerialName("logoUrl")
    val logoUrl: String? = null,
    @SerialName("websiteUrl")
    val websiteUrl: String? = null,
    @SerialName("minConfirmations")
    val minConfirmations: Int = 0,
    @SerialName("parentChain")
    val parentChain: String? = null,
    @SerialName("chainIdentifier")
    val chainIdentifier: String? = null,
    @SerialName("explorerUrl")
    val explorerUrl: String? = null
) {
    fun hasNonCustodialSupport(): Boolean =
        products.any { it == DynamicAssetProducts.DynamicSelfCustody || it == DynamicAssetProducts.PrivateKey }
}

data class DetailedAssetInformation(
    val description: String,
    val website: String,
    val whitepaper: String
)

typealias DynamicAssetList = List<DynamicAsset>

class AssetDiscoveryApiService internal constructor(
    private val api: AssetDiscoveryApiInterface,
    private val coinNetworkApi: CoinNetworkApiInterface
) {

    fun getFiatAssets(): Single<DynamicAssetList> =
        api.getFiatCurrencies()
            .map { dto ->
                dto.currencies.mapNotNull { it.toDynamicAsset() }
            }

    fun getEthErc20s(): Single<DynamicAssetList> =
        api.getEthErc20s().map { dto ->
            dto.currencies.mapNotNull { it.toDynamicAsset() }
        }

    fun getOtherNetworksErc20s(): Single<DynamicAssetList> =
        api.getOtherErc20s().map { dto ->
            dto.currencies.mapNotNull { it.toDynamicAsset() }
        }

    fun getCustodialAssets(): Single<DynamicAssetList> =
        api.getCustodialCurrencies()
            .map { dto ->
                dto.currencies.mapNotNull { it.toDynamicAsset() }
            }

    fun getL1Coins(): Single<DynamicAssetList> =
        api.getL1Coins()
            .map { dto ->
                dto.currencies.mapNotNull { it.toDynamicAsset() }
            }

    suspend fun allNetworks(): Outcome<Exception, List<CoinNetworkDto>> =
        coinNetworkApi.getCoinNetworks()
            .map { response ->
                response.networks.filter { it.type != NetworkType.NOT_SUPPORTED }
            }

    suspend fun allCoinTypes(): Outcome<Exception, List<CoinTypeDto>> =
        coinNetworkApi.getCoinNetworks().map { response ->
            response.types.filter { it.type != NetworkType.NOT_SUPPORTED }
        }

    suspend fun getAssetInformation(assetTicker: String): Outcome<Exception, AssetInformationDto> =
        api.getAssetInfo(assetTicker)

    private fun DynamicCurrency.toDynamicAsset(): DynamicAsset? =
        when {
            coinType is CeloTokenAsset && coinType.parentChain != CELO -> null
            coinType is UnsupportedAsset -> null
            else -> DynamicAsset(
                assetName = assetName,
                networkTicker = networkSymbol,
                displayTicker = displaySymbol,
                isFiat = coinType is FiatAsset,
                precision = precision,
                products = if (networkSymbol == "STX" || networkSymbol == "SOL") {
                    makeProductSet(products).plus(DynamicAssetProducts.DynamicSelfCustody)
                } else {
                    makeProductSet(products)
                },
                logoUrl = coinType.logoUrl,
                websiteUrl = coinType.websiteUrl,
                minConfirmations = when (coinType) {
                    is Erc20Asset -> ERC20_CONFIRMATIONS
                    is CoinAsset -> coinType.minConfirmations
                    is CeloTokenAsset -> CELO_CONFIRMATIONS
                    else -> 0
                },
                parentChain = when (coinType) {
                    is CeloTokenAsset -> coinType.parentChain
                    is Erc20Asset -> coinType.parentChain
                    else -> null
                },
                chainIdentifier = when (coinType) {
                    is CeloTokenAsset -> coinType.chainIdentifier
                    is Erc20Asset -> coinType.chainIdentifier
                    else -> null
                }
            )
        }

    private fun makeProductSet(products: List<String>): Set<DynamicAssetProducts> =
        products.mapNotNull {
            try {
                DynamicAssetProducts.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()

    private fun isParentChainEnabledEvm(evmChains: List<AssetInfo>, parentChain: String) =
        evmChains.find {
            it.networkTicker == parentChain || it.displayTicker == parentChain
        } != null

    companion object {
        const val CELO = "CELO"
        private const val ERC20_CONFIRMATIONS = 12
        private const val CELO_CONFIRMATIONS = 1
    }
}
