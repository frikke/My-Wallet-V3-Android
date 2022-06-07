package piuk.blockchain.android.rating.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RespondentResponse(
    @SerialName("Data") val data: RespondentResponseData
)

@Serializable
data class RespondentResponseData(
    @SerialName("Succeeded") val succeeded: Boolean
)