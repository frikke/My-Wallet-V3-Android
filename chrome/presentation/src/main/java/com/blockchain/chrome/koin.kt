package com.blockchain.chrome

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val multiAppModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            MultiAppViewModel(
                walletModeService = get(),
                walletModeBalanceService = get(),
                backupPhraseService = get(),
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
