package com.blockchain.chrome

import com.blockchain.koin.dexFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val multiAppModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            MultiAppViewModel(
                walletModeService = get(),
                walletModeBalanceService = get(),
                walletStatusPrefs = get(),
                dexFeatureFlag = get(dexFeatureFlag),
                walletModePrefs = get(),
                userFeaturePermissionService = get(),
                appRatingService = get()
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
                walletConnectV2Service = get(),
                walletConnectV2UrlValidator = get(),
            )
        }
    }
}
