package com.blockchain.home.presentation.koin

import com.blockchain.home.presentation.activity.detail.ActivityDetailViewModel
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.quickactions.QuickActionsViewModel
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.superAppModeService
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val homePresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            AssetsViewModel(
                homeAccountsService = get(),
                currencyPrefs = get(),
                exchangeRates = get(),
                filterService = get()
            )
        }

        viewModel {
            PrivateKeyActivityViewModel(
                unifiedActivityService = get()
            )
        }

        viewModel {
            CustodialActivityViewModel(
                coincore = get()
            )
        }

        viewModel {
            ActivityDetailViewModel()
        }
        viewModel {
            QuickActionsViewModel(
                walletModeService = get(superAppModeService),
                userFeaturePermissionService = get(),
            )
        }
    }
}
