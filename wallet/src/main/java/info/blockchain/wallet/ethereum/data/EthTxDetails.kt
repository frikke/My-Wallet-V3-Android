package info.blockchain.wallet.ethereum.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
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
class EthTxDetails {
    @JsonProperty("hash")
    var hash: String? = null

    @JsonProperty("nonce")
    var nonce: Long? = null

    @JsonProperty("blockHash")
    var blockHash: String? = null

    @JsonProperty("blockNumber")
    var blockNumber: Long? = null

    @JsonProperty("transactionIndex")
    var transactionIndex: Long? = null

    @JsonProperty("from")
    var from: String? = null

    @JsonProperty("to")
    var to: String? = null

    @JsonProperty("value")
    var value: BigInteger? = null

    @JsonProperty("gasPrice")
    var gasPrice: BigInteger? = null

    @JsonProperty("gas")
    var gas: BigInteger? = null

    @JsonProperty("input")
    var input: String? = null

    @JsonProperty("creates")
    var creates: String? = null

    @JsonProperty("publicKey")
    var publicKey: String? = null

    @JsonProperty("raw")
    var raw: String? = null

    @JsonProperty("r")
    var r: String? = null

    @JsonProperty("s")
    var s: String? = null

    @JsonProperty("v")
    var v: Long? = null

    @JsonProperty("nonceRaw")
    var nonceRaw: String? = null

    @JsonProperty("blockNumberRaw")
    var blockNumberRaw: String? = null

    @JsonProperty("transactionIndexRaw")
    var transactionIndexRaw: String? = null

    @JsonProperty("valueRaw")
    var valueRaw: String? = null

    @JsonProperty("gasPriceRaw")
    var gasPriceRaw: String? = null

    @JsonProperty("gasRaw")
    var gasRaw: String? = null
}
