package piuk.blockchain.android.everypay.models

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardDetailRequest(
    @field:Json(name = "api_username")
    @SerialName("api_username")
    val apiUsername: String,

    @field:Json(name = "cc_details")
    @SerialName("cc_details")
    val ccDetails: CcDetails,

    @field:Json(name = "nonce")
    @SerialName("nonce")
    val nonce: String,

    @field:Json(name = "token_consented")
    @SerialName("token_consented")
    val tokenConsented: Boolean = true,

    @field:Json(name = "mobile_token")
    @SerialName("mobile_token")
    val mobileToken: String,

    @field:Json(name = "timestamp")
    @SerialName("timestamp")
    val timestamp: String
)

@Serializable
data class CcDetails(
    @field:Json(name = "cc_number")
    @SerialName("cc_number")
    val number: String,

    @field:Json(name = "cvc")
    @SerialName("cvc")
    val cvc: String,

    @field:Json(name = "month")
    @SerialName("month")
    val month: String,

    @field:Json(name = "year")
    @SerialName("year")
    val year: String,

    @field:Json(name = "holder_name")
    @SerialName("holder_name")
    val holderName: String
)

@Serializable
data class CardDetailResponse(
    @field:Json(name = "payment_state")
    @SerialName("payment_state")
    val status: EveryPayPaymentStatus? = null
) {
    // If the received payment state is null or is set to "failed", then the payment authorization failed.
    val isSuccess: Boolean = status?.let { paymentStatus ->
        paymentStatus != EveryPayPaymentStatus.FAILED ||
            paymentStatus != EveryPayPaymentStatus.UNKNOWN
    } ?: false
}

@Serializable
enum class EveryPayPaymentStatus {
    @Json(name = "initial")
    @SerialName("initial")
    INITIAL,

    @Json(name = "settled")
    @SerialName("settled")
    SETTLED,

    @Json(name = "failed")
    @SerialName("failed")
    FAILED,

    @Json(name = "waiting_for_3ds_response")
    @SerialName("waiting_for_3ds_response")
    WAITING_FOR_3DS_RESPONSE,

    @Json(name = "waiting_for_sca")
    @SerialName("waiting_for_sca")
    WAITING_FOR_SCA,

    @Json(name = "confirmed_3ds")
    @SerialName("confirmed_3ds")
    CONFIRMED_3DS,

    UNKNOWN
}
