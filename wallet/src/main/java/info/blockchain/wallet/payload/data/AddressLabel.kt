package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressLabel(
    @SerialName("index")
    private val _index: Int? = null,
    @SerialName("label")
    val label: String
) {
    val index: Int
        get() = _index ?: 0
}
