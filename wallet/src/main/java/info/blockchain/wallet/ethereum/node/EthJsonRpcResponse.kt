package info.blockchain.wallet.ethereum.node

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EthJsonRpcResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("result")
    val result: String
)
