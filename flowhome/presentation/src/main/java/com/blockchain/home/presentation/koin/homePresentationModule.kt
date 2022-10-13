package com.blockchain.home.presentation.koin

import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val homePresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            AssetsViewModel(
                homeAccountsService = get(),
                currencyPrefs = get(),
                exchangeRates = get()
            )
        }
    }
}
