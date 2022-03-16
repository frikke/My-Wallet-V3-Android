package com.blockchain.core.eligibility

import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.services.ProductEligibilityService
import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.mapLeft
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class ProductsEligibilityCache(
    private val authenticator: Authenticator,
    private val service: ProductEligibilityService
) {

    private fun refresh(): Single<ProductEligibilityResponse> = rxSingleOutcome {
        service.getProductEligibility(authenticator.getAuthHeader().await())
            .mapLeft { it.throwable }
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    fun productsEligibility(): Single<ProductEligibilityResponse> = cache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 5L
    }
}
