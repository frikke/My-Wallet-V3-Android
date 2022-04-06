package com.blockchain.nabu.datamanagers

import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibility
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

interface SimpleBuyEligibilityProvider {
    fun isEligibleForSimpleBuy(forceRefresh: Boolean = false): Single<Boolean>
    fun simpleBuyTradingEligibility(): Single<SimpleBuyEligibility>
}

class NabuCachedEligibilityProvider(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) : SimpleBuyEligibilityProvider {

    private val refresh: () -> Single<SimpleBuyEligibility> = {
        authenticator.authenticate {
            nabuService.isEligibleForSimpleBuy(it)
        }
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = DEFAULT_CACHE_LIFETIME,
        refreshFn = refresh
    )

    override fun isEligibleForSimpleBuy(forceRefresh: Boolean): Single<Boolean> {
        if (forceRefresh) {
            cache.invalidate()
        }
        return cache.getCachedSingle().map {
            it.simpleBuyTradingEligible
        }.onErrorReturn {
            false
        }
    }

    override fun simpleBuyTradingEligibility(): Single<SimpleBuyEligibility> = cache.getCachedSingle()
}

const val DEFAULT_CACHE_LIFETIME = 20L
