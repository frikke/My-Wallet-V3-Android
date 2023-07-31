package com.blockchain.prices.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.prices.prices.PricesViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val pricesPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            PricesViewModel(
                walletModeService = get(),
                currencyPrefs = get(),
                userFeaturePermissionService = get(),
                pricesService = get(),
                dispatcher = Dispatchers.IO
            )
        }
    }
}
