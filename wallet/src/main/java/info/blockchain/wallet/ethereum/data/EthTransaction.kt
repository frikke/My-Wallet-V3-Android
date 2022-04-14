@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.wallet.ethereum.data

import com.blockchain.serializers.BigIntSerializer
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
class EthTransaction(

    @SerialName("blockNumber")
    val blockNumber: Long? = 0L,

    @SerialName("timestamp")
    val timestamp: Long = 0L,

    @SerialName("hash")
    val hash: String = "",

    @SerialName("nonce")
    val nonce: String = "",

    @SerialName("blockHash")
    val blockHash: String? = "",

    @SerialName("transactionIndex")
    val transactionIndex: Int = 0,

    @SerialName("from")
    val from: String = "",

    @SerialName("to")
    val to: String = "",

    @SerialName("value")
    val value: BigInteger = 0.toBigInteger(),

    @SerialName("gasPrice")
    val gasPrice: BigInteger = 0.toBigInteger(),

    @SerialName("gasUsed")
    val gasUsed: BigInteger = 0.toBigInteger(),

    @SerialName("state")
    val state: String = ""
)

enum class TransactionState {
    CONFIRMED,
    REPLACED,
    PENDING,
    UNKNOWN
}

@Serializable
class EthTransactionsResponse(
    @SerialName("transactions")
    val transactions: List<EthTransaction> = emptyList()
)
