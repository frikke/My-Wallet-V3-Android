package info.blockchain.wallet.payload.data

import com.blockchain.extensions.replace
import java.lang.IllegalStateException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountV4 constructor(
    @SerialName("label")
    override val label: String,
    /**
     * There are some payloads out there that are missing `default_derivation` and `archived`due to a default
     * value set once upon a time that was never encoded. :- That's why we have to make those private and
     * create baking fields for accessing
     */
    @SerialName("default_derivation")
    private val _defaultType: String? = null,
    @SerialName("archived")
    private val _isArchived: Boolean? = null,

    @SerialName("derivations")
    val derivations: List<Derivation>
) : Account {

    val defaultType: String
        get() = _defaultType ?: ""

    override val isArchived: Boolean
        get() = _isArchived ?: false

    override fun xpubForDerivation(derivation: String): String? =
        derivationForType(derivation)?.xpub

    override fun containsXpub(xpub: String): Boolean =
        derivations.map { it.xpub }.contains(xpub)

    private fun derivationForType(type: String) = derivations.find { it.type == type }

    private val derivation
        get() = derivationForType(defaultType)

    override val xpriv: String
        get() = derivation?.xpriv ?: ""

    override fun withEncryptedPrivateKey(encryptedKey: String): Account {
        return derivation?.let {
            return this.copy(
                derivations = derivations.replace(it, it.copy(xpriv = encryptedKey))
            )
        } ?: this
    }

    override val xpubs: XPubs by lazy {
        XPubs(derivations.map { XPub(address = it.xpub, derivation = mapFormat(it.type)) })
    }

    override val addressLabels: List<AddressLabel>
        get() = derivation?.addressLabels ?: listOf()

    override fun addAddressLabel(index: Int, reserveLabel: String): Account {
        val addressLabel = AddressLabel(index = index, label = reserveLabel)

        return derivation?.let { derivation ->
            this.copy(
                derivations = derivations.replace(
                    derivation,
                    derivation.copy(
                        _addressLabels = derivation.addressLabels.plus(addressLabel).toSet().toList()
                    )
                )
            )
        } ?: this
    }

    override fun updateLabel(label: String): Account = this.copy(
        label = label
    )

    override fun updateArchivedState(isArchived: Boolean): AccountV4 =
        this.copy(
            _isArchived = isArchived
        )

    private fun mapFormat(type: String): XPub.Format =
        when (type) {
            Derivation.LEGACY_TYPE -> XPub.Format.LEGACY
            Derivation.SEGWIT_BECH32_TYPE -> XPub.Format.SEGWIT
            else -> throw IllegalStateException("Unknown derivation type")
        }
}
