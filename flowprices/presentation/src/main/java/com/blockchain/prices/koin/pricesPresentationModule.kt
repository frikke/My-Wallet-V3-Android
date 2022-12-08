package com.blockchain.prices.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.superAppModeService
import com.blockchain.prices.prices.PricesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val pricesPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            PricesViewModel(
                walletModeService = get(superAppModeService),
                currencyPrefs = get(),
                coincore = get(),
                pricesPrefs = get(),
                exchangeRatesDataManager = get(),
                custodialWalletManager = get(),
                simpleBuyService = get(),
                watchlistService = get()
            )
        }
    }
}
