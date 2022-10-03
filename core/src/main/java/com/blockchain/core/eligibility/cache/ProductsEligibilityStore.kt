package com.blockchain.core.eligibility.cache

import com.blockchain.api.services.EligibilityApiService
import com.blockchain.core.eligibility.mapper.toDomain
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class ProductsEligibilityStore(
    private val authenticator: Authenticator,
    private val productEligibilityApi: EligibilityApiService
) : Store<ProductsEligibilityData> by PersistedJsonSqlDelightStoreBuilder().build(
    storeId = STORE_ID,
    fetcher = Fetcher.ofOutcome {
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap {
                productEligibilityApi.getProductEligibility(it)
                    .map {
                        val majorBlocked = it.notifications.map { it.toDomain() }
                        val products = it.toDomain().associateBy { it.product }
                        ProductsEligibilityData(majorBlocked, products)
                    }
            }
    },
    dataSerializer = ProductsEligibilityData.serializer(),
    mediator = FreshnessMediator(Freshness.ofMinutes(15))
) {
    fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "ProductsEligibilityStore"
    }
}
