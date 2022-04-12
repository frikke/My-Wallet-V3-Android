package info.blockchain.wallet.api.data

import java.util.HashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias Product = String
typealias Asset = String
typealias WalletAddress = String

@Serializable
class WalletOptions {

    @SerialName("showBuySellTab")
    val buySellCountries: List<String> = emptyList()

    @SerialName("androidBuyPercent")
    val rolloutPercentage: Double = 0.toDouble()

    @SerialName("android")
    var androidFlags: MutableMap<String, Boolean> = mutableMapOf()

    @SerialName("ethereum")
    val ethereum: EthereumOptions = EthereumOptions()

    @SerialName("hotWalletAddresses")
    val hotWalletAddresses: Map<Product, Map<Asset, WalletAddress>> = HashMap()

    @SerialName("android_update")
    val androidUpdate: AndroidUpgrade = AndroidUpgrade()

    @SerialName("mobileInfo")
    val mobileInfo: Map<String, String> = HashMap()

    @SerialName("bcash")
    private val bitcoinCashFees = HashMap<String, Int>()

    @SerialName("xlm")
    private val xlm: XlmOptions = XlmOptions()

    @SerialName("xlmExchange")
    private val xlmExchange: XlmExchange = XlmExchange()

    @SerialName("mobile")
    private val mobile = HashMap<String, String>()

    @SerialName("domains")
    private val domains = HashMap<String, String>()

    /**
     * Returns the XLM transaction timeout in seconds.
     *
     * See: https://github.com/stellar/stellar-core/issues/1811
     *
     * @return the timeout
     */
    val xlmTransactionTimeout: Long
        get() = xlm.sendTimeOutSeconds

    val xmlExchangeAddresses: List<String>
        get() = xlmExchange.exchangeAddresses

    val stellarHorizonUrl: String
        get() = domains["stellarHorizon"] ?: ""

    val buyWebviewWalletLink: String?
        get() = mobile["walletRoot"]

    companion object {
        var XLM_DEFAULT_TIMEOUT_SECS: Long? = 10L
    }
}
