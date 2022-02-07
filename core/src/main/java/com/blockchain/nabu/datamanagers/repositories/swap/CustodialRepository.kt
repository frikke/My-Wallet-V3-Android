package com.blockchain.nabu.datamanagers.repositories.swap

import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.TransferDirection
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class CustodialRepository(pairsProvider: TradingPairsProvider, activityProvider: SwapActivityProvider) {

    private val pairsCache = TimedCacheRequest(
        cacheLifetimeSeconds = LONG_CACHE,
        refreshFn = {
            pairsProvider.getAvailablePairs()
        }
    )

    private val swapActivityCache = TimedCacheRequest(
        cacheLifetimeSeconds = SHORT_CACHE,
        refreshFn = {
            activityProvider.getSwapActivity()
        }
    )

    fun getSwapAvailablePairs(): Single<List<CurrencyPair>> =
        pairsCache.getCachedSingle()

    fun getCustodialActivityForAsset(
        cryptoCurrency: AssetInfo,
        directions: Set<TransferDirection>
    ): Single<List<TradeTransactionItem>> =
        swapActivityCache.getCachedSingle().map { list ->
            list.filter {
                it.sendingValue.currency == cryptoCurrency &&
                    directions.contains(it.direction)
            }
        }

    companion object {
        const val LONG_CACHE = 60000L
        const val SHORT_CACHE = 120L
    }
}
