package info.blockchain.wallet.ethereum

import java.util.ArrayList
import java.util.HashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EthereumWalletData {

    @SerialName("has_seen")
    var hasSeen: Boolean = false

    @SerialName("default_account_idx")
    var defaultAccountIdx: Int = 0

    @SerialName("accounts")
    var accounts: ArrayList<EthereumAccount>? = null

    @SerialName("erc20")
    var erc20Tokens: HashMap<String, Erc20TokenData>? = null

    @SerialName("legacy_account")
    var legacyAccount: EthereumAccount? = null

    @SerialName("tx_notes")
    var txNotes: HashMap<String, String>? = null

    @SerialName("last_tx")
    var lastTx: String? = null

    @SerialName("last_tx_timestamp")
    var lastTxTimestamp: Long? = 0
}
