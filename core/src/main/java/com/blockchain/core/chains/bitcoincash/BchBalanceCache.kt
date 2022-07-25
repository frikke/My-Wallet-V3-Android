package com.blockchain.core.chains.bitcoincash

import com.blockchain.core.common.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Balance
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class BchBalanceCache(private val payloadDataManager: PayloadDataManager) {
    private val bchBalanceCache = ParameteredSingleTimedCacheRequest<List<XPubs>, Map<String, Balance>>(
        cacheLifetimeSeconds = CustodialRepository.LONG_CACHE,
        refreshFn = {
            payloadDataManager.getBalanceOfBchAccounts(it).firstOrError()
        }
    )

    fun invalidate() {
        bchBalanceCache.invalidateAll()
    }

    fun getBalanceOfAddresses(xPubs: List<XPubs>): Single<Map<String, Balance>> =
        bchBalanceCache.getCachedSingle(xPubs)
}
