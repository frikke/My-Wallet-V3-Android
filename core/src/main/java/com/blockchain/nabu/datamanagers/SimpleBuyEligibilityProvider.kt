package com.blockchain.nabu.datamanagers

import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

@Deprecated("usee SimpleBuyService")
interface SimpleBuyEligibilityProvider {
    @Deprecated("usee SimpleBuyService")
    fun isEligibleForSimpleBuy(forceRefresh: Boolean = false): Single<Boolean>

    @Deprecated("usee SimpleBuyService")
    fun simpleBuyTradingEligibility(): Single<SimpleBuyEligibilityDto>
}

class NabuCachedEligibilityProvider(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) : SimpleBuyEligibilityProvider {

    private val refresh: () -> Single<SimpleBuyEligibilityDto> = {
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

    override fun simpleBuyTradingEligibility(): Single<SimpleBuyEligibilityDto> = cache.getCachedSingle()
}

const val DEFAULT_CACHE_LIFETIME = 20L
