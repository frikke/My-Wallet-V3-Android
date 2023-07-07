package com.blockchain.coincore.loader

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single

/*
* from the unified balances endpoint, fetches the assets that user HAD a non custodial balance. we
* need that in order to display unsupported funded assets
* */
internal class HistoricActiveBalancesRepository(private val activeBalancesStore: ActiveBalancesStore) {
    private lateinit var activeCurrencies: List<String>
    fun currencyWasFunded(currency: Currency): Single<Boolean> {
        if (::activeCurrencies.isInitialized) {
            return Single.just(currency.networkTicker in activeCurrencies)
        }
        return activeBalancesStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)).asSingle()
            .map { balancesResp ->
                balancesResp.balances.filter { entry -> entry.balance?.amount?.signum() == 1 }
                    .map { balance -> balance.currency }
            }.doAfterSuccess {
                println("LALALA ACTIVE --- $activeCurrencies")
                activeCurrencies = it
            }.onErrorReturn {
                emptyList()
            }.map {
                currency.networkTicker in it
            }
    }
}
