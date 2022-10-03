package info.blockchain.wallet.dynamicselfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinConfiguration(
    @SerialName("coinType")
    val coinType: Int,
    @SerialName("purpose")
    val purpose: Int
)
