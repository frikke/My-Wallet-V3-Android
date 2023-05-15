package com.blockchain.kycproviders.prove.data.mapper

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.kyc.model.AddressResponse
import com.blockchain.api.kyc.model.PossessionStateResponse
import com.blockchain.api.kyc.model.PrefillDataResponse
import com.blockchain.api.kyc.model.StartInstantLinkAuthResponse
import com.blockchain.kycproviders.prove.domain.model.Address
import com.blockchain.kycproviders.prove.domain.model.PossessionState
import com.blockchain.kycproviders.prove.domain.model.PrefillData
import com.blockchain.kycproviders.prove.domain.model.StartInstantLinkAuthResult
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.fold

internal fun Outcome<
    Exception,
    PossessionStateResponse
    >.toPossessionStateDomainOutcome(): Outcome<Exception, PossessionState> =
    fold(
        onSuccess = {
            if (it.isVerified) {
                Outcome.Success(PossessionState.Verified(it.mobileNumber.orEmpty()))
            } else Outcome.Success(PossessionState.Unverified)
        },
        onFailure = { error ->
            if (error is NabuApiException && error.getErrorCode() == NabuErrorCodes.ProvePossessionFailed) {
                Outcome.Success(PossessionState.Failed)
            } else {
                Outcome.Failure(error)
            }
        }
    )

internal fun StartInstantLinkAuthResponse.toDomain(): StartInstantLinkAuthResult = StartInstantLinkAuthResult(
    smsRetryInSeconds = smsRetryInSeconds
)

internal fun PrefillDataResponse.toDomain(): PrefillData = PrefillData(
    firstName = firstName,
    lastName = lastName,
    addresses = addresses.map { it.toDomain() },
    dob = dob,
    phoneNumber = phoneNumber
)

internal fun AddressResponse.toDomain(): Address = Address(
    line1 = line1,
    line2 = line2,
    city = city,
    state = state,
    postCode = postCode,
    country = country
)
