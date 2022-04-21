package com.blockchain.nabu.cache

import com.blockchain.core.common.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

class UserCache(
    private val nabuService: NabuService
) {

    private val cache = ParameteredSingleTimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    private fun refresh(token: NabuSessionTokenResponse): Single<NabuUser> =
        nabuService.getUser(token)

    fun cached(token: NabuSessionTokenResponse): Single<NabuUser> =
        cache.getCachedSingle(token)

    companion object {
        private const val CACHE_LIFETIME = 20L
    }
}
