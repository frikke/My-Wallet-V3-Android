package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressLabel(

    @SerialName("index")
    var index: Int = 0,

    @SerialName("label")
    var label: String? = null
)
