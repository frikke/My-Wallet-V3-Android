package com.blockchain.core.eligibility.cache

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.ProductEligibilityApiService
import com.blockchain.core.eligibility.mapper.toDomain
import com.blockchain.core.eligibility.mapper.toError
import com.blockchain.domain.eligibility.model.EligibilityError
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class ProductsEligibilityStore(
    private val authenticator: Authenticator,
    private val productEligibilityApi: ProductEligibilityApiService
) : Store<
    EligibilityError,
    ProductsEligibilityData
    > by PersistedJsonSqlDelightStoreBuilder().build(
    storeId = STORE_ID,
    fetcher = Fetcher.ofOutcome {
        authenticator.getAuthHeader().awaitOutcome()
            .mapError(Throwable::toError)
            .flatMap {
                productEligibilityApi.getProductEligibility(it)
                    .map {
                        val majorBlocked = it.notifications.map { it.toDomain() }
                        val products = it.toDomain().associateBy { it.product }
                        ProductsEligibilityData(majorBlocked, products)
                    }
                    .mapError(ApiError::toError)
            }
    },
    dataSerializer = ProductsEligibilityData.serializer(),
    mediator = FreshnessMediator(Freshness.ofSeconds(5))
) {
    companion object {
        private const val STORE_ID = "ProductsEligibilityStore"
    }
}
