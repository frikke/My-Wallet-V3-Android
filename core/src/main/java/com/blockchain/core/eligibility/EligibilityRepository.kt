package com.blockchain.core.eligibility

import com.blockchain.core.eligibility.cache.ProductsEligibilityStore
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibilityError
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.store.StoreRequest
import com.blockchain.store.firstOutcome
import io.reactivex.rxjava3.core.Single
import java.util.Locale

class EligibilityRepository(
    private val productsEligibilityStore: ProductsEligibilityStore
) : EligibilityService {
    override fun getCustodialEligibleCountries(): Single<List<CountryIso>> = Single.just(
        Locale.getISOCountries()
            .toList()
            .filterNot { SANCTIONED_COUNTRIES_ISO.contains(it) }
    )

    override suspend fun getProductEligibility(product: EligibleProduct):
        Outcome<EligibilityError, ProductEligibility> =
        productsEligibilityStore.stream(StoreRequest.Cached(false))
            .firstOutcome()
            .map { data ->
                data.products[product] ?: ProductEligibility.asEligible(product)
            }

    override suspend fun getMajorProductsNotEligibleReasons():
        Outcome<EligibilityError, List<ProductNotEligibleReason>> =
        productsEligibilityStore.stream(StoreRequest.Cached(false))
            .firstOutcome()
            .map { data -> data.majorProductsNotEligibleReasons }

    companion object {
        private val SANCTIONED_COUNTRIES_ISO: List<CountryIso> = listOf("CU", "IR", "KP", "SY")
    }
}
