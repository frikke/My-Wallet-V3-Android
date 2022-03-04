package info.blockchain.wallet.ethereum.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigInteger

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
class EthAddressResponse {
    @JsonProperty("id")
    private val id: Int? = null

    @field:JsonProperty("txn_count")
    private val txnCount: Int? = null

    @JsonProperty("account")
    private val account: String? = null

    @JsonProperty("accountType")
    private val accountType: Int? = null

    @JsonProperty("balance")
    private var balance: BigInteger? = null

    @JsonProperty("nonce")
    private val nonce: Int? = null

    @JsonProperty("firstTime")
    private val firstTime: Long? = null

    @JsonProperty("numNormalTxns")
    private val numNormalTxns: Int? = null

    @JsonProperty("numInternalTxns")
    private val numInternalTxns: Int? = null

    @JsonProperty("totalReceived")
    private val totalReceived: BigInteger? = null

    @JsonProperty("totalSent")
    private val totalSent: BigInteger? = null

    @JsonProperty("totalFee")
    private val totalFee: BigInteger? = null

    @field:JsonProperty("txns")
    val transactions: List<EthTransaction> = emptyList()

    @JsonProperty("txnOffset")
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

    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}
