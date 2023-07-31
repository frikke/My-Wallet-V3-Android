package com.blockchain.prices

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.prices.domain.PricesService
import org.koin.dsl.bind
import org.koin.dsl.module

val pricesDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            PricesRepository(
                coincore = get(),
                exchangeRatesDataManager = get(),
                simpleBuyService = get(),
                watchlistService = get(),
                remoteConfigService = get()
            )
        }.bind(PricesService::class)
    }
}
