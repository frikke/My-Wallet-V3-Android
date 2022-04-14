package info.blockchain.wallet.ethereum.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EthPushTxRequest(
    @SerialName("rawTx")
    val rawTx: String? = null,
    @SerialName("api_code")
    val apiCode: String? = null
)
