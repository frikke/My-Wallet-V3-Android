package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountV3(
    @SerialName("label")
    override var label: String = "",

    @SerialName("archived")
    override var isArchived: Boolean = false,

    @SerialName("xpriv")
    override var xpriv: String = "",

    @SerialName("xpub")
    val legacyXpub: String = ""
) : Account {

    @delegate:Transient
    override val xpubs: XPubs by lazy {
        XPubs(XPub(address = legacyXpub, derivation = XPub.Format.LEGACY))
    }

    @SerialName("cache")
    override val addressCache: AddressCache = AddressCache()

    @SerialName("address_labels")
    override val addressLabels: MutableList<AddressLabel> = mutableListOf()

    override fun addAddressLabel(index: Int, reserveLabel: String) {
        val addressLabel = AddressLabel()
        addressLabel.label = reserveLabel
        addressLabel.index = index

        if (!addressLabels.contains(addressLabel)) {
            addressLabels.add(addressLabel)
        }
    }

    override fun upgradeToV4(): AccountV4 {
        val legacyDerivation = Derivation(
            Derivation.LEGACY_TYPE,
            Derivation.LEGACY_PURPOSE,
            xpriv,
            legacyXpub,
            addressCache,
            addressLabels
        )
        val derivations = mutableListOf(legacyDerivation)
        return AccountV4(label, legacyDerivation.type, isArchived, derivations)
    }

    override fun xpubForDerivation(derivation: String): String? =
        if (derivation == Derivation.LEGACY_TYPE) legacyXpub else null

    override fun containsXpub(xpub: String): Boolean {
        return legacyXpub == xpub
    }
}
