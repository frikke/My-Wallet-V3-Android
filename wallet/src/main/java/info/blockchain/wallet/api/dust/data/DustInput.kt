@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.wallet.api.dust.data

import com.blockchain.serializers.BigIntSerializer
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.TransactionOutPoint

@Serializable
data class DustInput(
    @SerialName("confirmations")
    val confirmations: Int,

    @SerialName("lock_secret")
    val lockSecret: String,

    @SerialName("output_script")
    val outputScript: String,

    @SerialName("script")
    val script: String,

    @SerialName("tx_hash")
    val txHash: String,

    @SerialName("tx_hash_big_endian")
    val txHashBigEndian: String,

    @SerialName("tx_index")
    val txIndex: Long,

    @SerialName("tx_output_n")
    val txOutputN: Long,

    @SerialName("value")
    val value: BigInteger,

    @SerialName("value_hex")
    val valueHex: String
) {

    fun getTransactionOutPoint(params: NetworkParameters): TransactionOutPoint = TransactionOutPoint(
        params,
        txOutputN,
        Sha256Hash.wrap(txHashBigEndian)
    )
}
