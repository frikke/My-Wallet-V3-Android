package piuk.blockchain.android.rating.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AppRatingApiKeys(
    @SerialName("surveyId") val surveyId: String,
    @SerialName("masterKey") val masterKey: String,
    @SerialName("key") val key: String
)