package com.blockchain.core.buy

import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsResp
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

class BuyPairsCache(private val nabuService: NabuService) {

    private val refresh: () -> Single<SimpleBuyPairsResp> = {
        nabuService.getSupportedCurrencies()
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = refresh
    )

    fun pairs(): Single<SimpleBuyPairsResp> = cache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 10 * 60L
    }
}
