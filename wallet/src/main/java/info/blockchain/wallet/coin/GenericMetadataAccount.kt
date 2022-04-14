package info.blockchain.wallet.coin

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * <p>
 *     Generic coin account data that can be stored in blockchain.info KV store.
 * </p>
 */
@Serializable
class GenericMetadataAccount(
    @SerialName("label")
    override var label: String = "",

    @SerialName("archived")
    var isArchived: Boolean = false,

    @SerialName("xpub")
    private var xpub: String? = null
) : JsonSerializableAccount {

    constructor(label: String, archived: Boolean) : this() {
        this.label = label
        isArchived = archived
    }

    fun setXpub(xpub: String?) {
        this.xpub = xpub
    }

    fun xpubs(): XPubs {
        return XPubs(XPub(xpub!!, XPub.Format.LEGACY))
    }
}
