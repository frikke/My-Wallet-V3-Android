package com.blockchain.core.buy

import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.BuyOrderListResponse
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

class BuyOrdersCache(private val authenticator: Authenticator, private val nabuService: NabuService) {

    private val refresh: () -> Single<BuyOrderListResponse> = {
        authenticator.authenticate {
            nabuService.getOutstandingOrders(
                sessionToken = it,
                pendingOnly = false
            )
        }
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = refresh
    )

    fun orders(): Single<BuyOrderListResponse> = cache.getCachedSingle()

    fun invalidate() {
        cache.invalidate()
    }

    companion object {
        private const val CACHE_LIFETIME = 1 * 60L
    }
}
