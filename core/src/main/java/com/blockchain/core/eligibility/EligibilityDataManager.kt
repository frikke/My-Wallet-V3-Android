package com.blockchain.core.eligibility

import com.blockchain.core.eligibility.cache.ProductsEligibilityCache
import com.blockchain.core.eligibility.mapper.toDomain
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.CountryIso
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.featureflag.FeatureFlag
import io.reactivex.rxjava3.core.Single
import java.util.Locale

class EligibilityDataManager(
    private val productsEligibilityCache: ProductsEligibilityCache,
    private val entitySwitchSilverEligibilityFeatureFlag: FeatureFlag
) : EligibilityService {
    override fun getCustodialEligibleCountries(): Single<List<CountryIso>> = Single.just(
        Locale.getISOCountries()
            .toList()
            .filterNot { SANCTIONED_COUNTRIES_ISO.contains(it) }
    )

    private fun getProductsEligibility(): Single<List<ProductEligibility>> =
        entitySwitchSilverEligibilityFeatureFlag.enabled
            .flatMap { enabled ->
                if (enabled) {
                    productsEligibilityCache.productsEligibility()
                        .map { response -> response.toDomain() }
                } else {
                    Single.just(emptyList())
                }
            }

    override fun getProductEligibility(product: EligibleProduct): Single<ProductEligibility> =
        getProductsEligibility().map { products ->
            products.find { it.product == product } ?: ProductEligibility.asEligible(product)
        }

    companion object {
        private val SANCTIONED_COUNTRIES_ISO: List<CountryIso> = listOf("CU", "IR", "KP", "SY")
    }
}
