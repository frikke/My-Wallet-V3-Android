package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Fee(
    @SerialName("fee")
    val fee: Double = 0.0,

    @SerialName("surge")
    val surge: Boolean = false,

    @SerialName("ok")
    val ok: Boolean = false
)
