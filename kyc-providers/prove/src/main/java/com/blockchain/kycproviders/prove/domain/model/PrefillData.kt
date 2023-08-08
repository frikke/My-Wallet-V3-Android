package com.blockchain.kycproviders.prove.domain.model

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso

// TODO(aromano): PROVE check nullability
data class PrefillData(
    val firstName: String?,
    val lastName: String?,
    val addresses: List<Address>?,
    val dob: String?, // ISO 8601
    val phoneNumber: String?
)

data class Address(
    val line1: String,
    val line2: String?,
    val city: String,
    val state: StateIso?,
    val postCode: String,
    val country: CountryIso?
)
