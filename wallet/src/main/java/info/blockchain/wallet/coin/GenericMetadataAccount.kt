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
data class GenericMetadataAccount(
    @SerialName("label")
    override val label: String,
    @SerialName("archived")
    private val _archived: Boolean? = null,
    @SerialName("xpub")
    private val _xpub: String? = null
) : JsonSerializableAccount {

    override val isArchived: Boolean
        get() = _archived ?: false

    private val xpub: String
        get() = _xpub ?: throw IllegalStateException("Xpub not presented")

    fun xpubs(): XPubs {
        return XPubs(XPub(xpub, XPub.Format.LEGACY))
    }

    fun updateXpub(xPub: String): GenericMetadataAccount =
        copy(
            _xpub = xPub
        )

    fun removeXpub(): GenericMetadataAccount =
        this.copy(_xpub = null)

    override fun updateArchivedState(isArchived: Boolean): GenericMetadataAccount {
        return copy(_archived = isArchived)
    }

    fun updateLabel(newLabel: String): GenericMetadataAccount = copy(
        label = newLabel
    )
}
