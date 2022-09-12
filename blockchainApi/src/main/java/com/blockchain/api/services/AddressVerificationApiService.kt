package com.blockchain.api.services

import com.blockchain.api.addressverification.AddressVerificationApi
import com.blockchain.api.addressverification.model.AutocompleteAddressResponse
import com.blockchain.api.addressverification.model.CompleteAddressDto
import com.blockchain.outcome.Outcome

class AddressVerificationApiService(
    private val api: AddressVerificationApi
) {

    suspend fun getAutocompleteAddresses(
        searchQuery: String,
        countryIso: String?,
        stateIso: String?,
        containerId: String?,
    ): Outcome<Exception, AutocompleteAddressResponse> = api.getAutocompleteAddresses(
        searchQuery,
        countryIso,
        stateIso,
        containerId
    )

    suspend fun getCompleteAddress(id: String): Outcome<Exception, CompleteAddressDto> =
        api.getCompleteAddress(id)
}
