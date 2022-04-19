package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeeLimits(
    @SerialName("min")
    val min: Long = 0,

    @SerialName("max")
    val max: Long = 0
)
