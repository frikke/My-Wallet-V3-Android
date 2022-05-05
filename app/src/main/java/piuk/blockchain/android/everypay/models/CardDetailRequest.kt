package piuk.blockchain.android.everypay.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardDetailRequest(
    @SerialName("api_username")
    val apiUsername: String,

    @SerialName("cc_details")
    val ccDetails: CcDetails,

    @SerialName("nonce")
    val nonce: String,

    @SerialName("token_consented")
    val tokenConsented: Boolean = true,

    @SerialName("mobile_token")
    val mobileToken: String,

    @SerialName("timestamp")
    val timestamp: String
)

@Serializable
data class CcDetails(
    @SerialName("cc_number")
    val number: String,

    @SerialName("cvc")
    val cvc: String,

    @SerialName("month")
    val month: String,

    @SerialName("year")
    val year: String,

    @SerialName("holder_name")
    val holderName: String
)

@Serializable
data class CardDetailResponse(
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
    @SerialName("initial")
    INITIAL,

    @SerialName("settled")
    SETTLED,

    @SerialName("failed")
    FAILED,

    @SerialName("waiting_for_3ds_response")
    WAITING_FOR_3DS_RESPONSE,

    @SerialName("waiting_for_sca")
    WAITING_FOR_SCA,

    @SerialName("confirmed_3ds")
    CONFIRMED_3DS,

    UNKNOWN
}
