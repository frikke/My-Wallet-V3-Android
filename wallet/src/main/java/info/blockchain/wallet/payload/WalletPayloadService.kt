package info.blockchain.wallet.payload

import info.blockchain.wallet.keys.MasterKey
import io.reactivex.rxjava3.core.Completable

interface WalletPayloadService {

    val password: String
    val guid: String
    val sharedKey: String
    val masterKey: MasterKey
    val isDoubleEncrypted: Boolean
    val initialised: Boolean
    val isBackedUp: Boolean
    fun updateMnemonicVerified(mnemonicVerified: Boolean): Completable
}
