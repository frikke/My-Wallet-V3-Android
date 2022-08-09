package com.blockchain.core.interest.data.datasources

import com.blockchain.api.interest.InterestApiService
import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class InterestAvailableAssetsTimedCache(
    private val authenticator: Authenticator,
    private val assetCatalogue: AssetCatalogue,
    private val interestApiService: InterestApiService
) {

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    private fun refresh(): Single<List<AssetInfo>> =
        authenticator.authenticate { token ->
            interestApiService.getAvailableTickersForInterest(token.authHeader).map { instrumentsResponse ->
                instrumentsResponse.networkTickers.mapNotNull { networkTicker ->
                    assetCatalogue.assetInfoFromNetworkTicker(networkTicker)
                }
            }
        }

    fun cached(): Single<List<AssetInfo>> = cache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 3600L
    }
}
