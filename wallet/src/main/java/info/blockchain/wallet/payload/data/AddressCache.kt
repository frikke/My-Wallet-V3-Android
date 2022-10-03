package info.blockchain.wallet.payload.data

import info.blockchain.wallet.bip44.HDAccount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
This class is used for iOS and Web only.
 */
@Serializable
data class AddressCache(
    @SerialName("receiveAccount")
    private val _receiveAccount: String? = null,
    @SerialName("changeAccount")
    private val _changeAccount: String? = null
) {
    val receiveAccount: String
        get() = _receiveAccount.orEmpty()

    val changeAccount: String
        get() = _changeAccount.orEmpty()

    companion object {
        fun setCachedXPubs(account: HDAccount): AddressCache {
            return AddressCache(
                _receiveAccount = account.receive.xpub,
                _changeAccount = account.change.xpub
            )
        }
    }
}
