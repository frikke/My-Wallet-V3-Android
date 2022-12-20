package com.blockchain.core

import com.blockchain.core.common.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

data class TransactionsRequest(
    val product: String,
    val type: String?,
)

class TransactionsCache(private val nabuService: NabuService) {

    private val refresh: (TransactionsRequest) -> Single<TransactionsResponse> = { request ->
        nabuService.getTransactions(
            product = request.product,
            type = request.type
        )
    }

    private val cache = ParameteredSingleTimedCacheRequest(
        cacheLifetimeSeconds = 60,
        refreshFn = refresh
    )

    fun transactions(transactionsRequest: TransactionsRequest): Single<TransactionsResponse> =
        cache.getCachedSingle(transactionsRequest)
}
