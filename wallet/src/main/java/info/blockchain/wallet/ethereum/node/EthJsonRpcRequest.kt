package info.blockchain.wallet.ethereum.node

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class RequestType(val methodName: String) {
    GET_BALANCE("eth_getBalance"),
    LATEST_BLOCK_NUMBER("eth_blockNumber"),
    IS_CONTRACT("eth_getCode"),
    GET_NONCE("eth_getTransactionCount")
}

@Serializable
class EthJsonRpcRequest(
    @SerialName("id")
    val id: Int = 0,
    @SerialName("method")
    val method: String,
    @SerialName("params")
    val params: List<String>
) {
    @SerialName("jsonrpc")
    val jsonrpc: String = "2.0"

    companion object {
        fun create(vararg params: String, type: RequestType) =
            EthJsonRpcRequest(
                method = type.methodName,
                params = params.toList()
            )

        @Transient
        const val defaultBlock = "latest"
    }
}
