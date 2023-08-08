package com.blockchain.api.kyc.model

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import kotlinx.serialization.Serializable

@Serializable
data class StartInstantLinkAuthResponse(
    val smsRetryInSeconds: Int,
    val smsLastSent: String // ISO 8601
)

@Serializable
data class StartMobileAuthResponse(
    val redirectTargetUrl: String
)

@Serializable
data class FinishMobileAuthResponse(
    val mobileNumber: String
)

@Serializable
data class PossessionStateResponse(
    val isVerified: Boolean,
    val mobileNumber: String?
)

@Serializable
data class PrefillDataResponse(
    val firstName: String,
    val lastName: String,
    val addresses: List<AddressResponse>,
    val dob: String, // ISO 8601
    val phoneNumber: String
)

@Serializable
data class AddressResponse(
    // TODO(aromano): PROVE sort out the nullability, these are all nullable coming from the BE
    val line1: String,
    val line2: String?,
    val city: String,
    val state: StateIso?,
    val postCode: String,
    val country: CountryIso?
)

@Serializable
data class PrefillDataSubmissionRequest(
    val firstName: String,
    val lastName: String,
    val address: AddressResponse,
    val dob: String, // ISO 8601
    val mobileNumber: String
)
