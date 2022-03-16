package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.HashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias Product = String
typealias Asset = String
typealias WalletAddress = String

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
class WalletOptions {

    @field:JsonProperty("showBuySellTab")
    @SerialName("showBuySellTab")
    val buySellCountries: List<String> = emptyList()

    @field:JsonProperty("androidBuyPercent")
    @SerialName("androidBuyPercent")
    val rolloutPercentage: Double = 0.toDouble()

    @field:JsonProperty("android")
    @SerialName("android")
    var androidFlags: MutableMap<String, Boolean> = mutableMapOf()

    @JsonProperty("ethereum")
    @SerialName("ethereum")
    val ethereum: EthereumOptions = EthereumOptions()

    @JsonProperty("hotWalletAddresses")
    @SerialName("hotWalletAddresses")
    val hotWalletAddresses: Map<Product, Map<Asset, WalletAddress>> = HashMap()

    @field:JsonProperty("android_update")
    @SerialName("android_update")
    val androidUpdate: AndroidUpgrade = AndroidUpgrade()

    @JsonProperty("mobileInfo")
    @SerialName("mobileInfo")
    val mobileInfo: Map<String, String> = HashMap()

    @field:JsonProperty("bcash")
    @SerialName("bcash")
    private val bitcoinCashFees = HashMap<String, Int>()

    @JsonProperty("xlm")
    @SerialName("xlm")
    private val xlm: XlmOptions = XlmOptions()

    @JsonProperty("xlmExchange")
    @SerialName("xlmExchange")
    private val xlmExchange: XlmExchange = XlmExchange()

    @JsonProperty("mobile")
    @SerialName("mobile")
    private val mobile = HashMap<String, String>()

    @JsonProperty("domains")
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
