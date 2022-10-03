package com.blockchain.core

import com.blockchain.core.common.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.models.responses.swap.CustodialOrderResponse
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

data class TransactionsRequest(
    val product: String,
    val type: String?,
)

class TransactionsCache(private val nabuService: NabuService, private val authenticator: Authenticator) {

    private val refresh: (TransactionsRequest) -> Single<TransactionsResponse> = { request ->
        authenticator.authenticate { token ->
            nabuService.getTransactions(
                sessionToken = token,
                product = request.product,
                type = request.type
            )
        }
    }

    private val cache = ParameteredSingleTimedCacheRequest(
        cacheLifetimeSeconds = 60,
        refreshFn = refresh
    )

    fun transactions(transactionsRequest: TransactionsRequest): Single<TransactionsResponse> =
        cache.getCachedSingle(transactionsRequest)
}

class SwapTransactionsCache(private val nabuService: NabuService, private val authenticator: Authenticator) {
    private val refresh: () -> Single<List<CustodialOrderResponse>> = {
        authenticator.authenticate { token ->
            nabuService.getSwapTrades(
                sessionToken = token
            )
        }
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = 20 * 60,
        refreshFn = refresh
    )

    fun swapOrders(): Single<List<CustodialOrderResponse>> =
        cache.getCachedSingle()

    fun invalidate() {
        cache.invalidate()
    }
}
