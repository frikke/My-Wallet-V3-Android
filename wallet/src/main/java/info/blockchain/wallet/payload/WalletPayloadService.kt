package info.blockchain.wallet.payload

import info.blockchain.wallet.keys.MasterKey

interface WalletPayloadService {

    val password: String
    val guid: String
    val sharedKey: String
    val masterKey: MasterKey
    val isDoubleEncrypted: Boolean
}
