package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XlmOptions(
    @SerialName("operationFee")
    val operationFee: Long = 0,
    @SerialName("sendTimeOutSeconds")
    val sendTimeOutSeconds: Long = 0
)
