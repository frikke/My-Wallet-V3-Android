package com.blockchain.chrome

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.superAppModeService
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val multiAppModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            MultiAppViewModel(
                walletModeService = get(superAppModeService),
                walletModeBalanceService = get(),
                payloadManager = get(),
                walletStatusPrefs = get(),
                walletModePrefs = get()
            )
        }

        viewModel {
            DeeplinkNavigationHandler(
                userIdentity = get(),
                deeplinkRedirector = get(),
                deeplinkService = get(),
                cancelOrderUseCase = get(),
                bankService = get(),
                bankBuyNavigation = get(),
                bankLinkingPrefs = get(),
            )
        }
    }
}
