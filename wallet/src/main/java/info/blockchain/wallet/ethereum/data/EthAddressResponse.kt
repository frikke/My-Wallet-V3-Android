@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.wallet.ethereum.data

import com.blockchain.serializers.BigIntSerializer
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@SuppressWarnings("unused")
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
class EthAddressResponse {
    @JsonProperty("id")
    @SerialName("id")
    private val id: Int? = null

    @field:JsonProperty("txn_count")
    @SerialName("txn_count")
    private val txnCount: Int? = null

    @JsonProperty("account")
    @SerialName("account")
    private val account: String? = null

    @JsonProperty("accountType")
    @SerialName("accountType")
    private val accountType: Int? = null

    @JsonProperty("balance")
    @SerialName("balance")
    private var balance: BigInteger? = null

    @JsonProperty("nonce")
    @SerialName("nonce")
    private val nonce: Int? = null

    @JsonProperty("firstTime")
    @SerialName("firstTime")
    private val firstTime: Long? = null

    @JsonProperty("numNormalTxns")
    @SerialName("numNormalTxns")
    private val numNormalTxns: Int? = null

    @JsonProperty("numInternalTxns")
    @SerialName("numInternalTxns")
    private val numInternalTxns: Int? = null

    @JsonProperty("totalReceived")
    @SerialName("totalReceived")
    private val totalReceived: BigInteger? = null

    @JsonProperty("totalSent")
    @SerialName("totalSent")
    private val totalSent: BigInteger? = null

    @JsonProperty("totalFee")
    @SerialName("totalFee")
    private val totalFee: BigInteger? = null

    @field:JsonProperty("txns")
    @SerialName("txns")
    val transactions: List<EthTransaction> = ArrayList()

    @JsonProperty("txnOffset")
    @SerialName("txnOffset")
    private val txnOffset: Int? = null

    fun getId(): Int? {
        return id
    }

    fun getTransactionCount(): Int? {
        return txnCount
    }

    fun getAccount(): String? {
        return account
    }

    fun getAccountType(): Int? {
        return accountType
    }

    fun getBalance(): BigInteger? {
        return balance
    }

    fun getNonce(): Int? {
        return nonce
    }

    fun getFirstTime(): Long? {
        return firstTime
    }

    fun getNumberOfNormalTransactions(): Int? {
        return numNormalTxns
    }

    fun getNumberOfInternalTransactions(): Int? {
        return numInternalTxns
    }

    fun getTotalReceived(): BigInteger? {
        return totalReceived
    }

    fun getTotalSent(): BigInteger? {
        return totalSent
    }

    fun getTotalFee(): BigInteger? {
        return totalFee
    }

    fun getTransactionOffset(): Int? {
        return txnOffset
    }

    fun setBalance(balance: BigInteger?) {
        this.balance = balance
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}
