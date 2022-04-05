package com.blockchain.core

import com.blockchain.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.service.NabuService
import io.reactivex.rxjava3.core.Single

data class TransactionsRequest(
    val currency: String,
    val product: String,
    val type: String?
)

class TransactionsCache(private val nabuService: NabuService, private val authenticator: Authenticator) {

    private val refresh: (TransactionsRequest) -> Single<TransactionsResponse> = { request ->
        authenticator.authenticate { token ->
            nabuService.getTransactions(
                sessionToken = token,
                currency = request.currency,
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
