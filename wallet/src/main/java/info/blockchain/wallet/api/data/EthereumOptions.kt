package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EthereumOptions(
    @SerialName("lastTxFuse")
    val lastTxFuse: Long = 0
)
