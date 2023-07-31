package com.blockchain.addressverification.data

import com.blockchain.addressverification.data.mapper.toDomain
import com.blockchain.addressverification.domain.AddressVerificationService
import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.CompleteAddress
import com.blockchain.api.services.AddressVerificationApiService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import kotlinx.coroutines.flow.map

class AddressVerificationRepository(
    private val api: AddressVerificationApiService,
    private val userService: UserService
) : AddressVerificationService {

    override suspend fun getAutocompleteAddressesWithUserCountry(
        searchQuery: String,
        containerId: String?
    ): Outcome<Exception, List<AutocompleteAddress>> =
        userService.getUserResourceFlow(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .firstOutcome()
            .flatMap { user ->
                getAutocompleteAddresses(searchQuery, user.address?.countryCode, user.address?.stateIso, containerId)
            }

    override suspend fun getAutocompleteAddresses(
        searchQuery: String,
        countryIso: CountryIso?,
        stateIso: StateIso?,
        containerId: String?
    ): Outcome<Exception, List<AutocompleteAddress>> =
        api.getAutocompleteAddresses(
            searchQuery = searchQuery,
            countryIso = countryIso,
            stateIso = stateIso,
            containerId = containerId
        ).map {
            it.toDomain()
        }

    override suspend fun getCompleteAddress(id: String): Outcome<Exception, CompleteAddress> =
        api.getCompleteAddress(id = id)
            .map { it.toDomain() }
}
