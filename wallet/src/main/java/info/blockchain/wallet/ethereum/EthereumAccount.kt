package info.blockchain.wallet.ethereum

import com.blockchain.serialization.JsonSerializableAccount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import info.blockchain.wallet.ethereum.util.HashUtil
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.keys.SigningKeyImpl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE
)
@Serializable
class EthereumAccount : JsonSerializableAccount {

    @JsonProperty("archived")
    @SerialName("archived")
    private val archived: Boolean = false

    @JsonProperty("label")
    @SerialName("label")
    override var label = ""

    @field:JsonProperty("correct")
    @SerialName("correct")
    var isCorrect: Boolean = false

    @field:JsonProperty("addr")
    @SerialName("addr")
    var address: String = ""

    constructor() {
        // default constructor for Jackson
    }

    constructor(addressKey: ECKey) {
        this.address = Keys.toChecksumAddress(
            HashUtil.toHexString(
                computeAddress(addressKey.pubKeyPoint.getEncoded(false))
            )
        )
    }

    /**
     * Compute an address from an encoded public key.
     *
     * @param pubBytes an encoded (uncompressed) public key
     * @return 20-byte address
     */
    private fun computeAddress(pubBytes: ByteArray): ByteArray {
        return HashUtil.sha3omit12(pubBytes.copyOfRange(1, pubBytes.size))
    }

    fun signTransaction(transaction: RawTransaction, masterKey: MasterKey): ByteArray {
        val signingKey = deriveSigningKey(masterKey)
        val credentials = Credentials.create(signingKey.privateKeyAsHex)
        return TransactionEncoder.signMessage(transaction, credentials)
    }

    fun signMessage(data: ByteArray, masterKey: MasterKey): ByteArray {
        val signingKey = deriveSigningKey(masterKey)
        val credentials = Credentials.create(signingKey.privateKeyAsHex)
        val signedData = Sign.signPrefixedMessage(data, credentials.ecKeyPair)

        /**
         * SignedData consists of :  // 1 byte header V + 32 bytes for R + 32 bytes for S
         * We want to return a byte[] with the concatenation of those as the return array of the
         * message sign which will be size of 65 containing R + S + V
         */
        val retval = ByteArray(65)
        System.arraycopy(signedData.r, 0, retval, 0, 32)
        System.arraycopy(signedData.s, 0, retval, 32, 32)
        System.arraycopy(signedData.v, 0, retval, 64, 1)
        return retval
    }

    fun deriveSigningKey(masterKey: MasterKey): SigningKey =
        SigningKeyImpl(deriveECKey(masterKey.toDeterministicKey(), 0))

    fun withChecksummedAddress(): String =
        Keys.toChecksumAddress(this.address)

    fun isAddressChecksummed(): Boolean =
        address == this.withChecksummedAddress()

    companion object {
        private const val DERIVATION_PATH = "m/44'/60'/0'/0"
        private const val DERIVATION_PATH_PURPOSE = 44
        private const val DERIVATION_PATH_COIN = 60
        private const val CHANGE_INDEX = 0
        private const val ADDRESS_INDEX = 0

        fun deriveAccount(masterKey: DeterministicKey, accountIndex: Int, label: String): EthereumAccount {
            val ethereumAccount = EthereumAccount(deriveECKey(masterKey, accountIndex))
            ethereumAccount.label = label
            ethereumAccount.isCorrect = true
            return ethereumAccount
        }

        private fun deriveECKey(masterKey: DeterministicKey, accountIndex: Int): ECKey {

            val purposeKey =
                HDKeyDerivation.deriveChildKey(
                    masterKey,
                    DERIVATION_PATH_PURPOSE or ChildNumber.HARDENED_BIT
                )
            val rootKey = HDKeyDerivation.deriveChildKey(
                purposeKey,
                DERIVATION_PATH_COIN or ChildNumber.HARDENED_BIT
            )
            val accountKey = HDKeyDerivation.deriveChildKey(
                rootKey,
                accountIndex or ChildNumber.HARDENED_BIT
            )
            val changeKey = HDKeyDerivation.deriveChildKey(
                accountKey,
                CHANGE_INDEX
            )
            val addressKey = HDKeyDerivation.deriveChildKey(
                changeKey,
                ADDRESS_INDEX
            )

            return ECKey.fromPrivate(addressKey.privKeyBytes)
        }
    }
}
