package com.blockchain.core.payments

import com.blockchain.api.services.CollateralLocks
import com.blockchain.api.services.PaymentsService
import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single

class WithdrawLocksCache(
    private val authenticator: Authenticator,
    private val paymentsService: PaymentsService,
    private val currencyPrefs: CurrencyPrefs,
) {
    private val currency: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency

    private val refresh: () -> Single<CollateralLocks> = {
        authenticator.authenticate { token ->
            paymentsService.getWithdrawalLocks(
                token.authHeader,
                currency.networkTicker
            )
        }
    }

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = 10 * 60,
        refreshFn = refresh
    )

    fun withdrawLocks() = cache.getCachedSingle()
    // todo(antonis-bc) call this after successful buy
    // todo(othman-bc) build a proper cache
    fun invalidate() = cache.invalidate()
}
