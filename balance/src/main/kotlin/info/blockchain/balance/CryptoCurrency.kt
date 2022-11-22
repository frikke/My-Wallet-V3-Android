package info.blockchain.balance

import io.reactivex.rxjava3.core.Single
import java.io.Serializable

enum class AssetCategory {
    CUSTODIAL,
    NON_CUSTODIAL,
    DELEGATED_NON_CUSTODIAL
}

interface AssetInfo : Currency, Serializable {
    val requiredConfirmations: Int

    // If non-null, then this is an l2 asset, and this contains the ticker of the chain on which this is implemented?
    val l1chainTicker: String?

    // If non-null, then this an l2 asset and this is the id on the l1 chain. Ie contract address for erc20 assets
    val l2identifier: String?

    // Resources
    val txExplorerUrlBase: String?

    override val type: CurrencyType
        get() = CurrencyType.CRYPTO

    val isErc20: Boolean
}

val Currency.isCustodialOnly: Boolean
    get() = categories.size == 1 && categories.contains(AssetCategory.CUSTODIAL)

val AssetInfo.isNonCustodialOnly: Boolean
    get() = categories.size == 1 && categories.contains(AssetCategory.NON_CUSTODIAL)

val AssetInfo.isDelegatedNonCustodial: Boolean
    get() = categories.contains(AssetCategory.DELEGATED_NON_CUSTODIAL)

val AssetInfo.isNonCustodial: Boolean
    get() = categories.contains(AssetCategory.NON_CUSTODIAL) || isDelegatedNonCustodial

fun AssetInfo.l1chain(assetCatalogue: AssetCatalogue): AssetInfo? =
    l1chainTicker?.let { ticker ->
        assetCatalogue.fromNetworkTicker(ticker) as AssetInfo
    }

interface AssetCatalogue {
    val supportedFiatAssets: List<FiatCurrency>
    val supportedCryptoAssets: List<AssetInfo>
    val supportedCustodialAssets: List<AssetInfo>

    fun fromNetworkTicker(symbol: String): Currency?
    fun fiatFromNetworkTicker(symbol: String): FiatCurrency?
    fun assetInfoFromNetworkTicker(symbol: String): AssetInfo?
    fun assetFromL1ChainByContractAddress(l1chain: String, contractAddress: String): AssetInfo?
    fun supportedL2Assets(chain: AssetInfo): List<AssetInfo>
    fun availableL1Assets(): Single<List<AssetInfo>>
    fun otherEvmAssets(): Single<List<AssetInfo>>
}

open class CryptoCurrency(
    override val displayTicker: String,
    override val networkTicker: String,
    override val name: String,
    override val categories: Set<AssetCategory>,
    override val precisionDp: Int,
    override val startDate: Long? = null, // token price start times in epoch-seconds. null if charting not supported
    override val requiredConfirmations: Int,
    override val l1chainTicker: String? = null,
    override val l2identifier: String? = null,
    override val colour: String,
    override val logo: String = "",
    override val txExplorerUrlBase: String? = null,
    override val isErc20: Boolean = false
) : AssetInfo {

    override val symbol: String
        get() = displayTicker

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other !is AssetInfo -> false
            other.networkTicker == networkTicker &&
                other.l1chainTicker == l1chainTicker &&
                other.l2identifier == l2identifier -> true
            else -> false
        }

    override fun hashCode(): Int {
        var result = networkTicker.hashCode()
        result = 31 * result + (l1chainTicker?.hashCode() ?: 0)
        result = 31 * result + (l2identifier?.hashCode() ?: 0)
        return result
    }

    object BTC : CryptoCurrency(
        displayTicker = "BTC",
        networkTicker = "BTC",
        name = "Bitcoin",
        categories = setOf(AssetCategory.NON_CUSTODIAL, AssetCategory.CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 3,
        startDate = 1282089600L, // 2010-08-18 00:00:00 UTC
        colour = "#FF9B22",
        logo = "file:///android_asset/logo/bitcoin/logo.png",
        txExplorerUrlBase = "https://www.blockchain.com/btc/tx/"
    ) {
        override val index: Int
            get() = BITCOIN_ORDER_INDEX
    }

    object ETHER : CryptoCurrency(
        displayTicker = "ETH",
        networkTicker = "ETH",
        name = "Ethereum",
        categories = setOf(AssetCategory.NON_CUSTODIAL, AssetCategory.CUSTODIAL),
        precisionDp = 18,
        requiredConfirmations = 12,
        startDate = 1438992000L, // 2015-08-08 00:00:00 UTC
        colour = "#473BCB",
        logo = "file:///android_asset/logo/ethereum/logo.png",
        txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"
    ) {
        override val index: Int
            get() = ETHER_ORDER_INDEX
    }

    object BCH : CryptoCurrency(
        displayTicker = "BCH",
        networkTicker = "BCH",
        name = "Bitcoin Cash",
        categories = setOf(AssetCategory.NON_CUSTODIAL, AssetCategory.CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 3,
        startDate = 1500854400L, // 2017-07-24 00:00:00 UTC
        colour = "#8DC351",
        logo = "file:///android_asset/logo/bitcoin_cash/logo.png",
        txExplorerUrlBase = "https://www.blockchain.com/bch/tx/"
    ) {
        override val index: Int
            get() = BCH_ORDER_INDEX
    }

    object XLM : CryptoCurrency(
        displayTicker = "XLM",
        networkTicker = "XLM",
        name = "Stellar",
        categories = setOf(AssetCategory.NON_CUSTODIAL, AssetCategory.CUSTODIAL),
        precisionDp = 7,
        requiredConfirmations = 1,
        startDate = 1409875200L, // 2014-09-04 00:00:00 UTC
        colour = "#000000",
        logo = "file:///android_asset/logo/stellar/logo.png",
        txExplorerUrlBase = "https://stellarchain.io/tx/"
    ) {
        override val index: Int
            get() = XLM_ORDER_INDEX
    }
    companion object {
        // TODO(dtverdota): remove these once the coin networks feature flag is removed(enabled forever)
        const val AVAX = "AVAX"
        const val MATIC = "MATIC"
        const val BNB = "BNB"
        const val MATIC_ON_POLYGON = "MATIC.MATIC"
    }
}

private const val BITCOIN_ORDER_INDEX = 4
private const val ETHER_ORDER_INDEX = 3
private const val BCH_ORDER_INDEX = 2
private const val XLM_ORDER_INDEX = 1
