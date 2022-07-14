package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressLabel(
    @SerialName("index")
    val index: Int,
    @SerialName("label")
    val label: String
)
