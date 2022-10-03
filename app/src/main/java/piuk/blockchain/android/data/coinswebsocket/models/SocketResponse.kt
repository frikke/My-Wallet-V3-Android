package piuk.blockchain.android.data.coinswebsocket.models

import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocketResponse(
    val success: Boolean? = true,
    val entity: Entity? = null,
    val coin: Coin? = null,
    val block: EthBlock? = null,
    val message: String? = null,
    val checksum: String? = null,
    val op: String? = null
)

@Serializable
data class BtcBchResponse(
    val transaction: BtcTransaction? = null
)

@Serializable
data class EthResponse(
    val transaction: EthTransaction? = null,
    val account: EthAccount? = null,
    val entity: Entity? = null,
    val tokenTransfer: TokenTransfer? = null,
    val tokenAccount: TokenAccount? = null,
    val tokenAccountKey: TokenAccountKey? = null,
    val param: TokenParams? = null
)

@Serializable
data class TokenParams(
    val tokenAddress: String? = null,
    val accountAddress: String? = null
)

@Serializable
data class BtcTransaction(
    val inputs: List<Input> = emptyList(),
    @SerialName("out")
    val outputs: List<Output> = emptyList(),
    val hash: String? = null
)

@Serializable
data class Output(
    val spent: Boolean,
    val addr: String? = null,
    val xpub: String? = null,
    val value: @Contextual BigDecimal? = null
)

@Serializable
data class Input(
    val address: String,
    val value: @Contextual BigInteger,
    @SerialName("prev_out")
    val prevOut: Output? = null
)

@Serializable
data class EthBlock(val hash: String, val parentHash: String, val nonce: String, val gasLimit: Long)

@Serializable
data class EthAccount(val address: String, val txHash: String, val tx: EthTransaction)

@Serializable
data class EthTransaction(
    val hash: String,
    val blockHash: String? = null,
    val blockNumber: Long? = null,
    val from: String,
    val to: String,
    val value: @Contextual BigInteger,
    val state: TransactionState
)

@Serializable
data class TokenAccount(
    val accountHash: String,
    val tokenHash: String,
    val balance: String,
    val totalSent: String
)

@Serializable
data class TokenTransfer(
    val blockHash: String,
    val transactionHash: String,
    val blockNumber: String,
    val tokenHash: String,
    val logIndex: String,
    val from: String,
    val to: String,
    val value: @Contextual BigInteger,
    val timeStamp: Long
)

@Serializable
data class TokenAccountKey(
    val accountHash: String,
    val tokenHash: String
)

@Serializable
enum class TransactionState {
    @SerialName("pending")
    PENDING,
    @SerialName("replaced")
    REPLACED,
    @SerialName("confirmed")
    CONFIRMED
}
