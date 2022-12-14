package com.blockchain.chrome

import com.blockchain.chrome.tbr.FiatActionsViewModel
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.superAppModeService
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val multiAppModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            MultiAppViewModel(
                walletModeService = get(superAppModeService),
                walletModeBalanceService = get()
            )
        }

        viewModel {
            FiatActionsViewModel(
                fiatActions = get()
            )
        }
    }
}
