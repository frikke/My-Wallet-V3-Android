package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Derivation(
    @SerialName("type")
    val type: String = "",

    @SerialName("purpose")
    val purpose: Int = 0,

    @SerialName("xpriv")
    var xpriv: String = "",

    @SerialName("xpub")
    var xpub: String = "",

    @SerialName("cache")
    var cache: AddressCache = AddressCache(),

    @SerialName("address_labels")
    var addressLabels: MutableList<AddressLabel> = mutableListOf()
) {
    constructor(type: String, purpose: Int) : this(type, purpose, "", "")

    companion object {
        const val LEGACY_TYPE = "legacy"
        const val LEGACY_PURPOSE = 44

        const val SEGWIT_BECH32_TYPE = "bech32"
        const val SEGWIT_BECH32_PURPOSE = 84

        @JvmStatic
        fun create(xpriv: String, xpub: String, cache: AddressCache): Derivation {
            return Derivation(LEGACY_TYPE, LEGACY_PURPOSE, xpriv, xpub, cache)
        }

        @JvmStatic
        fun createSegwit(xpriv: String, xpub: String, cache: AddressCache): Derivation {
            return Derivation(SEGWIT_BECH32_TYPE, SEGWIT_BECH32_PURPOSE, xpriv, xpub, cache)
        }
    }
}
