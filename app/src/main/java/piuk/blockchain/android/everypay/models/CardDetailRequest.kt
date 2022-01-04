package piuk.blockchain.android.everypay.models

import com.squareup.moshi.Json

data class CardDetailRequest(
    @field:Json(name = "api_username") val apiUsername: String,
    @field:Json(name = "cc_details") val ccDetails: CcDetails,
    @field:Json(name = "nonce") val nonce: String,
    @field:Json(name = "token_consented") val tokenConsented: Boolean = true,
    @field:Json(name = "mobile_token") val mobileToken: String,
    @field:Json(name = "timestamp") val timestamp: String
)

data class CcDetails(
    @field:Json(name = "cc_number") val number: String,
    @field:Json(name = "cvc") val cvc: String,
    @field:Json(name = "month") val month: String,
    @field:Json(name = "year") val year: String,
    @field:Json(name = "holder_name") val holderName: String
)

data class CardDetailResponse(
    @field:Json(name = "payment_state") val status: EveryPayPaymentStatus?
) {
    // If the received payment state is null or is set to "failed", then the payment authorization failed.
    val isSuccess: Boolean = status?.let { paymentStatus ->
        paymentStatus != EveryPayPaymentStatus.FAILED ||
            paymentStatus != EveryPayPaymentStatus.UNKNOWN
    } ?: false
}

enum class EveryPayPaymentStatus {
    @Json(name = "initial")
    INITIAL,
    @Json(name = "settled")
    SETTLED,
    @Json(name = "failed")
    FAILED,
    @Json(name = "waiting_for_3ds_response")
    WAITING_FOR_3DS_RESPONSE,
    @Json(name = "waiting_for_sca")
    WAITING_FOR_SCA,
    @Json(name = "confirmed_3ds")
    CONFIRMED_3DS,
    UNKNOWN
}
