package com.blockchain.addressverification.data

import com.blockchain.addressverification.data.mapper.toDomain
import com.blockchain.addressverification.domain.AddressVerificationService
import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.CompleteAddress
import com.blockchain.api.services.AddressVerificationApiService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.store.firstOutcome
import com.blockchain.utils.awaitOutcome
import com.blockchain.utils.rxSingleOutcome
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AddressVerificationRepository(
    private val api: AddressVerificationApiService,
    private val userService: UserService,
) : AddressVerificationService {

    override suspend fun getAutocompleteAddressesWithUserCountry(
        searchQuery: String,
        containerId: String?
    ): Outcome<Exception, List<AutocompleteAddress>> =
        userService.getUserFlow(FreshnessStrategy.Cached(false))
            // TODO(aromano): remove mapping once UserService returns DataResources
            .map { DataResource.Data(it) as DataResource<NabuUser> }
            .catch { emit(DataResource.Error(it as Exception)) }
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
        rxSingleOutcome {
            api.getAutocompleteAddresses(
                searchQuery = searchQuery,
                countryIso = countryIso,
                stateIso = stateIso,
                containerId = containerId
            ).map {
                it.toDomain()
            }
        }.awaitOutcome()

    override suspend fun getCompleteAddress(id: String): Outcome<Exception, CompleteAddress> =
        rxSingleOutcome {
            api.getCompleteAddress(id = id)
                .map { it.toDomain() }
        }.awaitOutcome()
}
