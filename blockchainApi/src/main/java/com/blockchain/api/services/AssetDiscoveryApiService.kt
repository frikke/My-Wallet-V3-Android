package com.blockchain.api.services

import com.blockchain.api.assetdiscovery.AssetDiscoveryApiInterface
import com.blockchain.api.assetdiscovery.data.AssetInformationResponse
import com.blockchain.api.assetdiscovery.data.CeloTokenAsset
import com.blockchain.api.assetdiscovery.data.CoinAsset
import com.blockchain.api.assetdiscovery.data.DynamicCurrency
import com.blockchain.api.assetdiscovery.data.Erc20Asset
import com.blockchain.api.assetdiscovery.data.FiatAsset
import com.blockchain.api.assetdiscovery.data.UnsupportedAsset
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import io.reactivex.rxjava3.core.Single

enum class DynamicAssetProducts {
    PrivateKey,
    MercuryDeposits, // Can create an address and deposit in the exchange
    MercuryWithdrawals, // Can withdraw funds from the exchange
    ServicePrice, // Service price has prices for it
    HWS, // HotWalletService supports it
    CustodialWalletBalance, // Can have a custodial/simplebuy balance of this currency
    InterestBalance, // Can have an interest balance
    DynamicSelfCustody
}

data class DynamicAsset(
    val assetName: String,
    val networkTicker: String,
    val displayTicker: String,
    val isFiat: Boolean,
    val precision: Int,
    val products: Set<DynamicAssetProducts>,
    val logoUrl: String? = null,
    val websiteUrl: String? = null,
    val minConfirmations: Int = 0,
    val parentChain: String? = null,
    val chainIdentifier: String? = null
)

data class DetailedAssetInformation(
    val description: String,
    val website: String,
    val whitepaper: String
)

typealias DynamicAssetList = List<DynamicAsset>

class AssetDiscoveryApiService internal constructor(
    private val api: AssetDiscoveryApiInterface
) {

    fun getFiatAssets(): Single<DynamicAssetList> =
        api.getFiatCurrencies()
            .map { dto ->
                dto.currencies.mapNotNull { it.toDynamicAsset() }
            }

    fun getErc20Assets(): Single<DynamicAssetList> =
        api.getErc20Currencies()
            .map { dto ->
                dto.currencies.mapNotNull { it.toDynamicAsset() }
            }

    fun getCustodialAssets(): Single<DynamicAssetList> =
        api.getCustodialCurrencies()
            .map { dto ->
                dto.currencies.mapNotNull { it.toDynamicAsset() }
            }

    suspend fun getL2AssetsForL1(l1Ticker: String): Outcome<Exception, DynamicAssetList> =
        api.getL2CurrenciesForL1(l1Ticker)
            .map { dto ->
                dto.currencies.mapNotNull { it.toDynamicAsset() }
            }

    suspend fun getAssetInformation(assetTicker: String): Outcome<Exception, DetailedAssetInformation?> =
        api.getAssetInfo(assetTicker).map {
            it.toAssetInfo()
        }

    private fun AssetInformationResponse.toAssetInfo(): DetailedAssetInformation? =
        if (description != null && website != null) {
            DetailedAssetInformation(
                description = description,
                website = website,
                whitepaper = whitepaper.orEmpty()
            )
        } else {
            null
        }

    private fun DynamicCurrency.toDynamicAsset(): DynamicAsset? =
        when {
            coinType is Erc20Asset && !supportedErc20Chains.contains(coinType.parentChain) -> null
            coinType is CeloTokenAsset && coinType.parentChain != CELO -> null
            coinType is UnsupportedAsset -> null
            else -> DynamicAsset(
                assetName = assetName,
                networkTicker = networkSymbol,
                displayTicker = displaySymbol,
                isFiat = coinType is FiatAsset,
                precision = precision,
                products = if (networkSymbol == "STX") {
                    // TODO(dtverdota): Remove once added on BE
                    makeProductSet(products).plus(DynamicAssetProducts.DynamicSelfCustody)
                } else {
                    makeProductSet(products)
                },
                logoUrl = coinType.logoUrl,
                websiteUrl = coinType.websiteUrl,
                minConfirmations = when (coinType) {
                    is Erc20Asset -> if (supportedErc20Chains.contains(coinType.parentChain)) {
                        ERC20_CONFIRMATIONS
                    } else {
                        throw IllegalStateException("Unknown parent chain")
                    }
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

    companion object {
        const val ETHEREUM = "ETH"
        const val MATIC = "MATIC"
        const val BNB = "BNB"
        val supportedErc20Chains = listOf(ETHEREUM, MATIC, BNB)
        const val CELO = "CELO"
        private const val ERC20_CONFIRMATIONS = 12
        private const val CELO_CONFIRMATIONS = 1
    }
}
