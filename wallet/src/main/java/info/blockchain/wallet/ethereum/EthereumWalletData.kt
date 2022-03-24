package info.blockchain.wallet.ethereum

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.ArrayList
import java.util.HashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE
)
@Serializable
class EthereumWalletData {

    @field:JsonProperty("has_seen")
    @SerialName("has_seen")
    var hasSeen: Boolean = false

    @field:JsonProperty("default_account_idx")
    @SerialName("default_account_idx")
    var defaultAccountIdx: Int = 0

    @JsonProperty("accounts")
    @SerialName("accounts")
    var accounts: ArrayList<EthereumAccount>? = null

    @field:JsonProperty("erc20")
    @SerialName("erc20")
    var erc20Tokens: HashMap<String, Erc20TokenData>? = null

    @field:JsonProperty("legacy_account")
    @SerialName("legacy_account")
    var legacyAccount: EthereumAccount? = null

    @field:JsonProperty("tx_notes")
    @SerialName("tx_notes")
    var txNotes: HashMap<String, String>? = null

    @field:JsonProperty("last_tx")
    @SerialName("last_tx")
    var lastTx: String? = null

    @field:JsonProperty("last_tx_timestamp")
    @SerialName("last_tx_timestamp")
    var lastTxTimestamp: Long? = 0
}
