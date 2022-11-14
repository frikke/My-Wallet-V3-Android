package com.blockchain.api.nabu.data

import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import kotlinx.serialization.Serializable

@Serializable
data class GeolocationResponse(
    val countryCode: CountryIso,
    val state: StateIso? = null,
)
