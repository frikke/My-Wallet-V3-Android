package info.blockchain.wallet.ethereum

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.wallet.LabeledAccount
import info.blockchain.wallet.ethereum.util.HashUtil
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.keys.SigningKeyImpl
import java.math.BigInteger
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.crypto.TransactionEncoder

class EthereumAccount(val ethAccountDto: EthAccountDto) : JsonSerializableAccount, LabeledAccount {

    constructor(addressKey: ECKey, label: String) : this(
        EthAccountDto.fromCheckSumAddress(
            address = Keys.toChecksumAddress(
                HashUtil.toHexString(
                    computeAddress(addressKey.pubKeyPoint.getEncoded(false))
                )
            ),
            label = label,
            pubKey = addressKey.publicKeyAsHex
        )
    )

    val publicKey: String?
        get() = ethAccountDto.publicKey

    fun withUpdatedLabel(label: String): EthereumAccount =
        EthereumAccount(ethAccountDto.copy(label = label))

    val address: String
        get() = ethAccountDto.address

    override val isArchived: Boolean
        get() = ethAccountDto.archived

    override val label: String
        get() = ethAccountDto.label

    override fun updateArchivedState(isArchived: Boolean): JsonSerializableAccount {
        throw UnsupportedOperationException("Cannot update label of $this")
    }

    fun signTransaction(transaction: RawTransaction, masterKey: MasterKey, chainId: Int): ByteArray {
        val signingKey = deriveSigningKey(masterKey)
        val credentials = Credentials.create(signingKey.privateKeyAsHex)
        // Have to pass the chain ID (1 for Ethereum) for the transaction to be replay-protected
        return TransactionEncoder.signMessage(transaction, chainId.toLong(), credentials)
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

    fun signEthTypedMessage(data: String, masterKey: MasterKey): ByteArray {
        val signingKey = deriveSigningKey(masterKey)
        val credentials = Credentials.create(signingKey.privateKeyAsHex)

        val structuredData = StructuredDataEncoder(data).hashStructuredData()
        val signedData = Sign.signMessage(structuredData, credentials.ecKeyPair, false)

        val retval = ByteArray(65)
        System.arraycopy(signedData.r, 0, retval, 0, 32)
        System.arraycopy(signedData.s, 0, retval, 32, 32)
        System.arraycopy(signedData.v, 0, retval, 64, 1)
        return retval
    }

    fun signRawPreImage(rawPreImage: String, masterKey: MasterKey): String {
        val signingKey = deriveSigningKey(masterKey).toECKey()
        return getSignature(rawPreImage.removePrefix("0x"), signingKey)
    }

    private fun getSignature(rawPreImage: String, signingKey: ECKey): String {
        val bytes = rawPreImage.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val sha256Hash = Sha256Hash.wrap(bytes)
        val resultSignature = signingKey.sign(sha256Hash)
        val r = resultSignature.r.toPaddedHexString()
        val s = resultSignature.s.toPaddedHexString()
        val v = "0${signingKey.findRecoveryId(sha256Hash, resultSignature)}"
        return r + s + v
    }

    fun deriveSigningKey(masterKey: MasterKey): SigningKey =
        SigningKeyImpl(deriveECKey(masterKey.toDeterministicKey(), 0))

    fun isAddressChecksummed(): Boolean =
        this.ethAccountDto.address == Keys.toChecksumAddress(this.ethAccountDto.address)

    companion object {
        private const val DERIVATION_PATH_PURPOSE = 44
        private const val DERIVATION_PATH_COIN = 60
        private const val CHANGE_INDEX = 0
        private const val ADDRESS_INDEX = 0

        fun deriveAccount(masterKey: DeterministicKey, accountIndex: Int, label: String): EthereumAccount {
            return EthereumAccount(addressKey = deriveECKey(masterKey, accountIndex), label = label)
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

        fun deriveECKey(masterKey: DeterministicKey, accountIndex: Int): ECKey {
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

private fun BigInteger.toPaddedHexString(): String {
    val radix = 16 // For digit to character conversion (digit to hexadecimal in this case)
    val desiredLength = 64
    val padChar = '0'
    return toString(radix).padStart(desiredLength, padChar)
}
