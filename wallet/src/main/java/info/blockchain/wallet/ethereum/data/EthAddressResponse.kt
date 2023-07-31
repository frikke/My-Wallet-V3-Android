@file:UseSerializers(BigIntSerializer::class)

package info.blockchain.wallet.ethereum.data

import com.blockchain.serializers.BigIntSerializer
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@SuppressWarnings("unused")
@Serializable
class EthAddressResponse {
    @SerialName("id")
    private val id: Int? = null

    @SerialName("txn_count")
    private val txnCount: Int? = null

    @SerialName("account")
    private val account: String? = null

    @SerialName("accountType")
    private val accountType: Int? = null

    @SerialName("balance")
    private var balance: BigInteger? = null

    @SerialName("nonce")
    private val nonce: Int? = null

    @SerialName("firstTime")
    private val firstTime: Long? = null

    @SerialName("numNormalTxns")
    private val numNormalTxns: Int? = null

    @SerialName("numInternalTxns")
    private val numInternalTxns: Int? = null

    @SerialName("totalReceived")
    private val totalReceived: BigInteger? = null

    @SerialName("totalSent")
    private val totalSent: BigInteger? = null

    @SerialName("totalFee")
    private val totalFee: BigInteger? = null

    @SerialName("txns")
    val transactions: List<EthTransaction> = ArrayList()

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
}
