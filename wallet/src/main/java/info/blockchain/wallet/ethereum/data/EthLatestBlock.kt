package info.blockchain.wallet.ethereum.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * We don't currently parse the transactions included in the block in this object.
 */
@Serializable
data class EthLatestBlock(
    @SerialName("number")
    val blockHeight: Long = 0
)
