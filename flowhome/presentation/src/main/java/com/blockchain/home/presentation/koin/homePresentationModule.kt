package com.blockchain.home.presentation.koin

import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailViewModel
import com.blockchain.home.presentation.activity.detail.privatekey.PrivateKeyActivityDetailViewModel
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
                filterService = get(),
                assetCatalogue = get()
            )
        }

        viewModel {
            PrivateKeyActivityViewModel(
                unifiedActivityService = get()
            )
        }

        viewModel {
            CustodialActivityViewModel(
                custodialActivityService = get()
            )
        }

        viewModel { (txId: String) ->
            PrivateKeyActivityDetailViewModel(
                activityTxId = txId,
                unifiedActivityService = get(),
                networkWalletService = get()
            )
        }

        viewModel { (txId: String) ->
            CustodialActivityDetailViewModel(
                activityTxId = txId,
                custodialActivityService = get(),
                paymentMethodService = get(),
                cardService = get(),
                bankService = get(),
                coincore = get(),
                defaultLabels = get()
            )
        }

        viewModel {
            QuickActionsViewModel(
                walletModeService = get(superAppModeService),
                userFeaturePermissionService = get(),
            )
        }
    }
}
