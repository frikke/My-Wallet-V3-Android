package com.blockchain.core.eligibility

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.api.services.EligibilityApiService
import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.core.eligibility.mapper.toDomain
import com.blockchain.core.eligibility.mapper.toNetwork
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.store.firstOutcome

class EligibilityRepository(
    private val productsEligibilityStore: ProductsEligibilityStore,
    private val eligibilityApiService: EligibilityApiService
) : EligibilityService {

    override suspend fun getCountriesList(
        scope: GetRegionScope
    ): Outcome<ApiException, List<Region.Country>> =
        eligibilityApiService.getCountriesList(scope.toNetwork())
            .map { countries -> countries.map(CountryResponse::toDomain) }

    override suspend fun getStatesList(
        countryCode: CountryIso,
        scope: GetRegionScope
    ): Outcome<ApiException, List<Region.State>> =
        eligibilityApiService.getStatesList(countryCode, scope.toNetwork())
            .map { states -> states.map(StateResponse::toDomain) }

    override suspend fun getProductEligibility(product: EligibleProduct):
        Outcome<Exception, ProductEligibility> =
        productsEligibilityStore.stream(FreshnessStrategy.Cached(false))
            .firstOutcome()
            .map { data ->
                data.products[product] ?: ProductEligibility.asEligible(product)
            }

    override suspend fun getMajorProductsNotEligibleReasons():
        Outcome<Exception, List<ProductNotEligibleReason>> =
        productsEligibilityStore.stream(FreshnessStrategy.Cached(false))
            .firstOutcome()
            .map { data -> data.majorProductsNotEligibleReasons }
}
