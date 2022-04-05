@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.wallet.ethereum.node

import com.blockchain.serializers.BigIntSerializer
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class EthTransactionResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("result")
    val result: EthTransaction
)

@Serializable
data class EthTransaction(
    @SerialName("blockHash")
    val blockHash: String? = "",

    @SerialName("blockNumber")
    val blockNumber: Long? = 0L,

    @SerialName("from")
    val from: String = "",

    @SerialName("gas")
    val gasUsed: BigInteger = 0.toBigInteger(),

    @SerialName("gasPrice")
    val gasPrice: BigInteger = 0.toBigInteger(),

    @SerialName("timestamp")
    val timestamp: Long = 0L,

    @SerialName("hash")
    val hash: String = "",

    @SerialName("input")
    val data: String,

    @SerialName("nonce")
    val nonce: String = "",

    @SerialName("to")
    val to: String = "",

    @SerialName("transactionIndex")
    val transactionIndex: Int = 0,

    @SerialName("value")
    val value: BigInteger = 0.toBigInteger(),
)
