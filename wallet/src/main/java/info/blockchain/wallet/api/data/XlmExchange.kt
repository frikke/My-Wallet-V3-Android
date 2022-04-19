package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XlmExchange(
    @SerialName("exchangeAddresses")
    val exchangeAddresses: List<String> = emptyList()
)
