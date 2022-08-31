package com.blockchain.api.services

import com.blockchain.api.addressverification.AddressVerificationApi
import com.blockchain.api.addressverification.model.AutocompleteAddressResponse
import com.blockchain.api.addressverification.model.CompleteAddressDto
import com.blockchain.outcome.Outcome

class AddressVerificationApiService(
    private val api: AddressVerificationApi
) {

    suspend fun getAutocompleteAddresses(
        authToken: String,
        searchQuery: String,
        countryIso: String?,
        stateIso: String?,
        containerId: String?,
    ): Outcome<Exception, AutocompleteAddressResponse> = api.getAutocompleteAddresses(
        authToken,
        searchQuery,
        countryIso,
        stateIso,
        containerId
    )

    suspend fun getCompleteAddress(
        authToken: String,
        id: String
    ): Outcome<Exception, CompleteAddressDto> = api.getCompleteAddress(
        authToken,
        id
    )
}
