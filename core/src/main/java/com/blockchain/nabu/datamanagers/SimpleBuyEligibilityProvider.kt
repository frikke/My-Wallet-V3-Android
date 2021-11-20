package com.blockchain.nabu.datamanagers

import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

interface SimpleBuyEligibilityProvider {
    fun isEligibleForSimpleBuy(forceRefresh: Boolean = false): Single<Boolean>
}

class NabuCachedEligibilityProvider(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) : SimpleBuyEligibilityProvider {

    private val refresh: () -> Single<Boolean> = {
        authenticator.authenticate {
            nabuService.isEligibleForSimpleBuy(it)
        }.map {
            it.simpleBuyTradingEligible
        }.onErrorReturn {
            false
        }
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = 20L,
        refreshFn = refresh
    )

    override fun isEligibleForSimpleBuy(forceRefresh: Boolean): Single<Boolean> {
        return if (!forceRefresh) cache.getCachedSingle() else refresh()
    }
}
