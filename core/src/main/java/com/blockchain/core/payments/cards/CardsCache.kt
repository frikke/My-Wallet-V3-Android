package com.blockchain.core.payments.cards

import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single

class CardsCache(
    private val paymentMethodsService: PaymentMethodsService,
    private val authenticator: Authenticator,
    private val stripeAndCheckoutFeatureFlag: FeatureFlag,
) {
    private val cardProvidersEnabled: Single<Boolean> by lazy {
        stripeAndCheckoutFeatureFlag.enabled.cache()
    }

    private val refresh: () -> Single<List<CardResponse>> = {
        cardProvidersEnabled.flatMap { cardProvidersEnabled ->
            authenticator.getAuthHeader().flatMap { authToken ->
                paymentMethodsService.getCards(
                    authorization = authToken,
                    cardProvidersSupported = cardProvidersEnabled
                )
            }
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
