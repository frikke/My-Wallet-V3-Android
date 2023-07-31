package info.blockchain.wallet.dynamicselfcustody

import com.blockchain.domain.wallet.CoinType
import info.blockchain.wallet.bip44.HDAddress
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.util.PrivateKeyFactory
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.stellar.sdk.KeyPair

class DynamicHDAccount(
    hdSeed: ByteArray,
    params: NetworkParameters,
    wKey: DeterministicKey,
    coinType: CoinType
) {

    private val coinDerivationKey =
        HDKeyDerivation.deriveChildKey(wKey, coinType.type or ChildNumber.HARDENED_BIT)
    private val accountDerivationKey =
        HDKeyDerivation.deriveChildKey(coinDerivationKey, 0 or ChildNumber.HARDENED_BIT)
    private val addressDerivationKey =
        HDKeyDerivation.deriveChildKey(accountDerivationKey, 0)

    val address = HDAddress(params, addressDerivationKey, 0, coinType.purpose)

    val bitcoinSerializedBase58Address: String
        get() = address.formattedAddress

    private val bip39Key: KeyPair = KeyPair.fromBip39Seed(hdSeed, 0)

    val bip39PubKey: ByteArray = bip39Key.publicKey

    val signingKey: SigningKey = PrivateKeyFactory().getSigningKey(
        PrivateKeyFactory.WIF_COMPRESSED,
        address.privateKeyString
    )

    fun signWithBip39Key(data: ByteArray): ByteArray = bip39Key.sign(data)
}
