package info.blockchain.wallet.payload.data

import info.blockchain.wallet.bip44.HDAccount
import java.lang.IllegalStateException
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountV4 @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("label")
    override var label: String = "",

    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("default_derivation")
    var defaultType: String = "",

    @SerialName("archived")
    override var isArchived: Boolean = false,

    @SerialName("derivations")
    val derivations: MutableList<Derivation> = mutableListOf()
) : Account {

    override fun xpubForDerivation(derivation: String): String? =
        derivationForType(derivation)?.xpub

    override fun containsXpub(xpub: String): Boolean =
        derivations.map { it.xpub }.contains(xpub)

    fun derivationForType(type: String) = derivations.find { it.type == type }

    private val derivation
        get() = derivationForType(defaultType)

    override var xpriv: String
        get() = derivation?.xpriv ?: ""
        set(value) {
            derivation?.xpriv = value
        }

    @delegate:Transient
    override val xpubs: XPubs by lazy {
        XPubs(derivations.map { XPub(address = it.xpub, derivation = mapFormat(it.type)) })
    }

    override val addressCache: AddressCache
        get() = derivation?.cache ?: AddressCache()

    override val addressLabels: MutableList<AddressLabel>
        get() = derivation?.addressLabels ?: mutableListOf()

    override fun addAddressLabel(index: Int, reserveLabel: String) {
        val addressLabel = AddressLabel().apply {
            this.index = index
            this.label = reserveLabel
        }
        if (derivation?.addressLabels?.contains(addressLabel) == true) {
            derivation?.addressLabels?.add(addressLabel)
        }
    }

    override fun upgradeToV4() = this

    @Deprecated("We should pass the info into the Account v3.upgrade method and keep this immutable if possible")
    fun addSegwitDerivation(hdAccount: HDAccount, index: Int) {
        if (defaultType == Derivation.SEGWIT_BECH32_TYPE) {
            return
        }
        derivations += Derivation.createSegwit(
            hdAccount.xPriv,
            hdAccount.xpub,
            AddressCache.setCachedXPubs(hdAccount)
        )
        defaultType = Derivation.SEGWIT_BECH32_TYPE
    }
}

private fun mapFormat(type: String): XPub.Format =
    when (type) {
        Derivation.LEGACY_TYPE -> XPub.Format.LEGACY
        Derivation.SEGWIT_BECH32_TYPE -> XPub.Format.SEGWIT
        else -> throw IllegalStateException("Unknown derivation type")
    }
