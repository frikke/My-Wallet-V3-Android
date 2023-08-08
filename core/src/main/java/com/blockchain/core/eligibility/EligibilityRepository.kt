package com.blockchain.core.eligibility

import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.api.services.EligibilityApiService
import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.core.eligibility.mapper.toDomain
import com.blockchain.core.eligibility.mapper.toNetwork
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.data.mapData
import com.blockchain.data.onErrorReturn
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import kotlinx.coroutines.flow.Flow

class EligibilityRepository(
    private val productsEligibilityStore: ProductsEligibilityStore,
    private val eligibilityApiService: EligibilityApiService
) : EligibilityService {

    override suspend fun getCountriesList(
        scope: GetRegionScope
    ): Outcome<Exception, List<Region.Country>> =
        eligibilityApiService.getCountriesList(scope.toNetwork())
            .map { countries -> countries.map(CountryResponse::toDomain) }

    override suspend fun getStatesList(
        countryCode: CountryIso,
        scope: GetRegionScope
    ): Outcome<Exception, List<Region.State>> =
        eligibilityApiService.getStatesList(countryCode, scope.toNetwork())
            .map { states -> states.map(StateResponse::toDomain) }

    override suspend fun getProductEligibilityLegacy(
        product: EligibleProduct,
        freshnessStrategy: FreshnessStrategy
    ): Outcome<Exception, ProductEligibility> =
        getProductEligibility(product, freshnessStrategy).firstOutcome()

    override fun getProductEligibility(
        product: EligibleProduct,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<ProductEligibility>> {
        return productsEligibilityStore.stream(freshnessStrategy)
            .mapData { eligibility ->
                eligibility.products[product] ?: ProductEligibility.asEligible(product)
            }.onErrorReturn {
                if (product == EligibleProduct.USE_CUSTODIAL_ACCOUNTS) {
                    ProductEligibility.asEligible(product)
                } else {
                    ProductEligibility.asNotEligible(product)
                }
            }
    }

    override suspend fun getMajorProductsNotEligibleReasons(): Outcome<Exception, List<ProductNotEligibleReason>> =
        productsEligibilityStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .firstOutcome()
            .map { data -> data.majorProductsNotEligibleReasons }
}
