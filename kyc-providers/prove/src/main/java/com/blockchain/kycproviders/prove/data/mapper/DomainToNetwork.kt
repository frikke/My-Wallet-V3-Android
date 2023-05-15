package com.blockchain.kycproviders.prove.data.mapper

import com.blockchain.api.kyc.model.AddressResponse
import com.blockchain.api.kyc.model.PrefillDataSubmissionRequest
import com.blockchain.kycproviders.prove.domain.model.Address
import com.blockchain.kycproviders.prove.domain.model.PrefillDataSubmission

internal fun PrefillDataSubmission.toNetwork(): PrefillDataSubmissionRequest = PrefillDataSubmissionRequest(
    firstName = firstName,
    lastName = lastName,
    address = address.toNetwork(),
    dob = dob,
    mobileNumber = mobileNumber
)

internal fun Address.toNetwork(): AddressResponse = AddressResponse(
    line1 = line1,
    line2 = line2,
    city = city,
    state = state,
    postCode = postCode,
    country = country
)
