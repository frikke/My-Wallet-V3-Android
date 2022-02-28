package com.blockchain.core.payments.cards

import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import io.reactivex.rxjava3.core.Single

class CardsCache(
    private val paymentMethodsService: PaymentMethodsService,
    private val authenticator: Authenticator,
) {

    private val refresh: () -> Single<List<CardResponse>> = {
        authenticator.getAuthHeader().flatMap { authToken ->
            paymentMethodsService.getCards(
                authorization = authToken,
                cardProvidersSupported = true
            )
        }
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = refresh
    )

    fun cards(): Single<List<CardResponse>> = cache.getCachedSingle()

    fun invalidate() {
        cache.invalidate()
    }

    companion object {
        private const val CACHE_LIFETIME = 1 * 60L
    }
}
