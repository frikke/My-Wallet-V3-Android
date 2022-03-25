@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.wallet.ethereum.data

import com.blockchain.serializers.BigIntSerializer
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
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
class EthTxDetails {
    @JsonProperty("hash")
    @SerialName("hash")
    var hash: String? = null

    @JsonProperty("nonce")
    @SerialName("nonce")
    var nonce: Long? = null

    @JsonProperty("blockHash")
    @SerialName("blockHash")
    var blockHash: String? = null

    @JsonProperty("blockNumber")
    @SerialName("blockNumber")
    var blockNumber: Long? = null

    @JsonProperty("transactionIndex")
    @SerialName("transactionIndex")
    var transactionIndex: Long? = null

    @JsonProperty("from")
    @SerialName("from")
    var from: String? = null

    @JsonProperty("to")
    @SerialName("to")
    var to: String? = null

    @JsonProperty("value")
    @SerialName("value")
    var value: BigInteger? = null

    @JsonProperty("gasPrice")
    @SerialName("gasPrice")
    var gasPrice: BigInteger? = null

    @JsonProperty("gas")
    @SerialName("gas")
    var gas: BigInteger? = null

    @JsonProperty("input")
    @SerialName("input")
    var input: String? = null

    @JsonProperty("creates")
    @SerialName("creates")
    var creates: String? = null

    @JsonProperty("publicKey")
    @SerialName("publicKey")
    var publicKey: String? = null

    @JsonProperty("raw")
    @SerialName("raw")
    var raw: String? = null

    @JsonProperty("r")
    @SerialName("r")
    var r: String? = null

    @JsonProperty("s")
    @SerialName("s")
    var s: String? = null

    @JsonProperty("v")
    @SerialName("v")
    var v: Long? = null

    @JsonProperty("nonceRaw")
    @SerialName("nonceRaw")
    var nonceRaw: String? = null

    @JsonProperty("blockNumberRaw")
    @SerialName("blockNumberRaw")
    var blockNumberRaw: String? = null

    @JsonProperty("transactionIndexRaw")
    @SerialName("transactionIndexRaw")
    var transactionIndexRaw: String? = null

    @JsonProperty("valueRaw")
    @SerialName("valueRaw")
    var valueRaw: String? = null

    @JsonProperty("gasPriceRaw")
    @SerialName("gasPriceRaw")
    var gasPriceRaw: String? = null

    @JsonProperty("gasRaw")
    @SerialName("gasRaw")
    var gasRaw: String? = null
}
