package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Status(
    @SerialName("success")
    var success: String = ""
)
