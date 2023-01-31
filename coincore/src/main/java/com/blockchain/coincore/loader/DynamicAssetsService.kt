package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.data.DataResource
import com.blockchain.domain.wallet.CoinNetwork
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface DynamicAssetsService {
    fun availableCryptoAssets(): Single<List<AssetInfo>>
    fun availableL1Assets(): Single<List<AssetInfo>>
    fun allEvmAssets(): Single<List<AssetInfo>>
    fun allEvmNetworks(): Single<List<EvmNetwork>>
    fun getEvmNetworkForCurrency(currency: String): Maybe<EvmNetwork>
    fun allNetworks(): Flow<DataResource<List<CoinNetwork>>>
}

internal fun DynamicAsset.toAssetInfo(evmNetworks: List<EvmNetwork> = emptyList()): AssetInfo =
    parentChain?.let { chain ->
        val pChain = evmNetworks.find { it.networkTicker == chain }?.networkTicker ?: kotlin.run {
            when (chain) {
                AssetDiscoveryApiService.CELO -> AssetDiscoveryApiService.CELO
                else -> null
            }
        }
        pChain?.let {
            CryptoCurrency(
                displayTicker = displayTicker,
                networkTicker = networkTicker,
                name = assetName,
                categories = mapCategories(products),
                precisionDp = precision,
                l1chainTicker = pChain,
                l2identifier = chainIdentifier,
                coinNetwork = evmNetworks.find { it.networkTicker == chain },
                requiredConfirmations = minConfirmations,
                startDate = BTC_START_DATE,
                colour = mapColour(),
                logo = logoUrl ?: "", // TODO: Um?
                txExplorerUrlBase = explorerUrl
            )
        }
    } ?: kotlin.run {
        // if the asset is a native/L1 token
        CryptoCurrency(
            displayTicker = displayTicker,
            networkTicker = networkTicker,
            name = assetName,
            categories = mapCategories(products),
            precisionDp = precision,
            l1chainTicker = null,
            l2identifier = chainIdentifier,
            requiredConfirmations = minConfirmations,
            startDate = BTC_START_DATE,
            colour = mapColour(),
            logo = logoUrl ?: "", // TODO: Um?
            txExplorerUrlBase = explorerUrl
        )
    }

private const val BTC_START_DATE = 1282089600L

private fun mapCategories(products: Set<DynamicAssetProducts>): Set<AssetCategory> =
    products.mapNotNull {
        when (it) {
            DynamicAssetProducts.PrivateKey -> AssetCategory.NON_CUSTODIAL
            DynamicAssetProducts.CustodialWalletBalance -> AssetCategory.CUSTODIAL
            DynamicAssetProducts.InterestBalance -> AssetCategory.CUSTODIAL
            DynamicAssetProducts.DynamicSelfCustody -> AssetCategory.DELEGATED_NON_CUSTODIAL
            else -> null
        }
    }.toSet()

private const val BLUE_600 = "#0C6CF2"
private fun DynamicAsset.mapColour(): String =
    when {
        colourLookup.containsKey(networkTicker) -> colourLookup[networkTicker] ?: BLUE_600
        chainIdentifier != null -> CryptoCurrency.ETHER.colour
        else -> BLUE_600
    }

private val colourLookup = mapOf(
    "BTC" to "#FF9B22",
    "BCH" to "#8DC351",
    "ETH" to "#473BCB",
    "XLM" to "#000000",
    "MATIC" to "#8247E5",
    "PAX" to "#00522C",
    "USDT" to "#26A17B",
    "WDGLD" to "#A39424",
    "AAVE" to "#2EBAC6",
    "YFI" to "#0074FA",
    "ALGO" to "#000000",
    "DOT" to "#E6007A",
    "DOGE" to "#C2A633",
    "CLOUT" to "#000000",
    "LTC" to "#BFBBBB",
    "ETC" to "#33FF99",
    "ZEN" to "#041742",
    "XTZ" to "#2C7DF7",
    "STX" to "#211F6D",
    "MOB" to "#243855",
    "THETA" to "#2AB8E6",
    "NEAR" to "#000000",
    "EOS" to "#000000",
    "OGN" to "#1A82FF",
    "ENJ" to "#624DBF",
    "COMP" to "#00D395",
    "LINK" to "#2A5ADA",
    "USDC" to "#2775CA",
    "UNI" to "#FF007A",
    "DAI" to "#F5AC37",
    "BAT" to "#FF4724"
)
