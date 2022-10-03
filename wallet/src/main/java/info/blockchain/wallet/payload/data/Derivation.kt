package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Derivation(
    @SerialName("type")
    val type: String,

    @SerialName("purpose")
    val purpose: Int,

    @SerialName("xpriv")
    val xpriv: String,

    @SerialName("xpub")
    val xpub: String,

    @SerialName("cache")
    val cache: AddressCache,

    @SerialName("address_labels")
    private val _addressLabels: List<AddressLabel>? = null
) {
    val addressLabels: List<AddressLabel>
        get() = _addressLabels ?: emptyList()

    companion object {
        const val LEGACY_TYPE = "legacy"
        const val LEGACY_PURPOSE = 44

        const val SEGWIT_BECH32_TYPE = "bech32"
        const val SEGWIT_BECH32_PURPOSE = 84

        @JvmStatic
        fun create(xpriv: String, xpub: String, cache: AddressCache): Derivation {
            return Derivation(
                type = LEGACY_TYPE,
                purpose = LEGACY_PURPOSE,
                xpriv = xpriv,
                xpub = xpub,
                cache = cache,
                _addressLabels = emptyList()
            )
        }

        @JvmStatic
        fun createSegwit(xpriv: String, xpub: String, cache: AddressCache): Derivation {
            return Derivation(
                type = SEGWIT_BECH32_TYPE,
                purpose = SEGWIT_BECH32_PURPOSE,
                xpriv = xpriv,
                xpub = xpub,
                cache = cache,
                _addressLabels = emptyList()
            )
        }
    }
}
