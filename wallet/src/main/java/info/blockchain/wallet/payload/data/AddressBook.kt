package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AddressBook(
    @SerialName("label")
    var label: String? = null,

    @SerialName("addr")
    var address: String? = null
)
