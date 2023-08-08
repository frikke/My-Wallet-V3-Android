package com.blockchain.coincore.loader

import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface DynamicAssetsService {
    fun availableCryptoAssets(): Single<List<AssetInfo>>
    fun allEvmNetworks(): Single<List<CoinNetwork>>
    fun allNetworks(): Flow<DataResource<List<CoinNetwork>>>
}

internal fun DynamicAsset.toAssetInfo(networks: List<CoinNetwork>): AssetInfo? {
    val coinNetwork = if (parentChain != null) {
        networks.find { network -> network.networkTicker == parentChain }
    } else networks.find { it.nativeAssetTicker == networkTicker }

    if (coinNetwork == null && hasNonCustodialSupport()) return null

    return CryptoCurrency(
        displayTicker = displayTicker,
        networkTicker = networkTicker,
        name = assetName,
        categories = mapCategories(products),
        precisionDp = precision,
        l2identifier = chainIdentifier,
        coinNetwork = coinNetwork,
        requiredConfirmations = minConfirmations,
        startDate = BTC_START_DATE,
        colour = mapColour(),
        logo = logoUrl ?: "",
        txExplorerUrlBase = explorerUrl
    )
}

private const val BTC_START_DATE = 1282089600L

private fun mapCategories(products: Set<DynamicAssetProducts>): Set<AssetCategory> =
    products.mapNotNull {
        when (it) {
            DynamicAssetProducts.PrivateKey -> AssetCategory.NON_CUSTODIAL
            DynamicAssetProducts.CustodialWalletBalance -> AssetCategory.TRADING
            DynamicAssetProducts.InterestBalance,
            DynamicAssetProducts.Staking,
            DynamicAssetProducts.EarnCC1W -> AssetCategory.INTEREST
            DynamicAssetProducts.DynamicSelfCustody -> AssetCategory.DELEGATED_NON_CUSTODIAL
            else -> null
        }
    }.toSet()

private const val BLUE_600 = "#0C6CF2"
private fun DynamicAsset.mapColour(): String =
    colourLookup[networkTicker] ?: parentChain?.let { colourLookup[it] ?: BLUE_600 } ?: BLUE_600

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
