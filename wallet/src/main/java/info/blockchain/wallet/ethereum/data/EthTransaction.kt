@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.wallet.ethereum.data

import com.blockchain.api.serializers.BigIntSerializer
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
@Serializable
class EthTransaction(

    @JsonProperty("blockNumber")
    @SerialName("blockNumber")
    val blockNumber: Long? = 0L,

    @JsonProperty("timestamp")
    @SerialName("timestamp")
    val timestamp: Long = 0L,

    @JsonProperty("hash")
    @SerialName("hash")
    val hash: String = "",

    @JsonProperty("nonce")
    @SerialName("nonce")
    val nonce: String = "",

    @JsonProperty("blockHash")
    @SerialName("blockHash")
    val blockHash: String? = "",

    @JsonProperty("transactionIndex")
    @SerialName("transactionIndex")
    val transactionIndex: Int = 0,

    @JsonProperty("from")
    @SerialName("from")
    val from: String = "",

    @JsonProperty("to")
    @SerialName("to")
    val to: String = "",

    @JsonProperty("value")
    @SerialName("value")
    val value: BigInteger = 0.toBigInteger(),

    @JsonProperty("gasPrice")
    @SerialName("gasPrice")
    val gasPrice: BigInteger = 0.toBigInteger(),

    @JsonProperty("gasUsed")
    @SerialName("gasUsed")
    val gasUsed: BigInteger = 0.toBigInteger(),

    @JsonProperty("state")
    @SerialName("state")
    val state: String = ""
)

enum class TransactionState {
    CONFIRMED,
    REPLACED,
    PENDING,
    UNKNOWN
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
@Serializable
class EthTransactionsResponse(
    @JsonProperty("transactions")
    @SerialName("transactions")
    val transactions: List<EthTransaction> = emptyList()
)
