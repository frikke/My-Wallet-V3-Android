package com.blockchain.addressverification.domain

import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.CompleteAddress
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.outcome.Outcome

interface AddressVerificationService {

    suspend fun getAutocompleteAddressesWithUserCountry(
        searchQuery: String,
        containerId: String?
    ): Outcome<Exception, List<AutocompleteAddress>>

    suspend fun getAutocompleteAddresses(
        searchQuery: String,
        countryIso: CountryIso?,
        stateIso: StateIso?,
        containerId: String?
    ): Outcome<Exception, List<AutocompleteAddress>>

    suspend fun getCompleteAddress(id: String): Outcome<Exception, CompleteAddress>
}
