package com.blockchain.nabu.datamanagers.custodialwalletimpl

import com.blockchain.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.cards.PaymentMethodResponse
import com.blockchain.nabu.service.NabuService
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single

class PaymentMethodsEligibilityCache(
    private val authenticator: Authenticator,
    private val service: NabuService
) {

    private val cache = ParameteredSingleTimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    data class Request(
        val currency: Currency,
        val eligibleOnly: Boolean,
        val shouldFetchSddLimits: Boolean
    )

    private fun refresh(request: Request): Single<List<PaymentMethodResponse>> =
        authenticator.authenticate {
            service.paymentMethods(
                sessionToken = it,
                currency = request.currency.networkTicker,
                eligibleOnly = request.eligibleOnly,
                tier = if (request.shouldFetchSddLimits) SDD_ELIGIBLE_TIER else null
            )
        }

    fun cached(request: Request): Single<List<PaymentMethodResponse>> =
        cache.getCachedSingle(request)

    companion object {
        private const val CACHE_LIFETIME = 20L
        private const val SDD_ELIGIBLE_TIER = 3
    }
}
