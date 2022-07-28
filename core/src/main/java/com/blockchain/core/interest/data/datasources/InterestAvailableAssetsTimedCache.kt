package com.blockchain.core.interest.data.datasources

import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class InterestAvailableAssetsTimedCache(
    private val authenticator: Authenticator,
    private val assetCatalogue: AssetCatalogue,
    private val nabuService: NabuService,
) {

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    private fun refresh(): Single<List<AssetInfo>> =
        authenticator.authenticate { token ->
            nabuService.getAvailableTickersForInterest(token).map { instrumentsResponse ->
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
