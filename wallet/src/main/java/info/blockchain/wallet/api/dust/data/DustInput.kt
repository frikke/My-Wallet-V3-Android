@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.wallet.api.dust.data

import com.blockchain.serializers.BigIntSerializer
import com.squareup.moshi.Json
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
    @field:Json(name = "confirmations")
    val confirmations: Int,

    @SerialName("lock_secret")
    @field:Json(name = "lock_secret")
    val lockSecret: String,

    @SerialName("output_script")
    @field:Json(name = "output_script")
    val outputScript: String,

    @SerialName("script")
    @field:Json(name = "script")
    val script: String,

    @SerialName("tx_hash")
    @field:Json(name = "tx_hash")
    val txHash: String,

    @SerialName("tx_hash_big_endian")
    @field:Json(name = "tx_hash_big_endian")
    val txHashBigEndian: String,

    @SerialName("tx_index")
    @field:Json(name = "tx_index")
    val txIndex: Long,

    @SerialName("tx_output_n")
    @field:Json(name = "tx_output_n")
    val txOutputN: Long,

    @SerialName("value")
    @field:Json(name = "value")
    val value: BigInteger,

    @SerialName("value_hex")
    @field:Json(name = "value_hex")
    val valueHex: String
) {

    fun getTransactionOutPoint(params: NetworkParameters): TransactionOutPoint = TransactionOutPoint(
        params,
        txOutputN,
        Sha256Hash.wrap(txHashBigEndian)
    )
}
