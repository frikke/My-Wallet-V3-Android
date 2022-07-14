package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountV3(
    @SerialName("label")
    override val label: String,
    @SerialName("archived")
    override val isArchived: Boolean,
    @SerialName("xpriv")
    override val xpriv: String,
    @SerialName("xpub")
    val legacyXpub: String,
    @SerialName("cache")
    val addressCache: AddressCache,
    @SerialName("address_labels")
    private val _addressLabels: List<AddressLabel>? = null
) : Account {

    override val addressLabels: List<AddressLabel>
        get() = _addressLabels ?: emptyList()

    override val xpubs: XPubs by lazy {
        XPubs(XPub(address = legacyXpub, derivation = XPub.Format.LEGACY))
    }

    override fun withEncryptedPrivateKey(encryptedKey: String): Account = this.copy(
        xpriv = encryptedKey
    )

    override fun updateLabel(label: String): Account {
        return this.copy(
            label = label
        )
    }

    override fun updateArchivedState(isArchived: Boolean): AccountV3 =
        this.copy(
            isArchived = isArchived
        )

    override fun addAddressLabel(index: Int, reserveLabel: String): Account {
        val addressLabel = AddressLabel(index, reserveLabel)
        if (_addressLabels?.contains(addressLabel) == true) {
            return this
        }
        return this.copy(
            _addressLabels = _addressLabels?.plus(addressLabel)?.toSet()?.toList()
        )
    }

    override fun xpubForDerivation(derivation: String): String? =
        if (derivation == Derivation.LEGACY_TYPE) legacyXpub else null

    override fun containsXpub(xpub: String): Boolean {
        return legacyXpub == xpub
    }
}
