package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.receive.accounts.ReceiveAccountsViewModel
import com.blockchain.transactions.receive.detail.ReceiveAccountDetailViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val receivePresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            ReceiveAccountsViewModel(
                walletModeService = get(),
                coincore = get(),
                assetCatalogue = get(),
                oneTimeAccountPersistenceService = get(),
                fiatCurrenciesService = get(),
                fiatActions = get()
            )
        }

        viewModel {
            ReceiveAccountDetailViewModel(
                oneTimeAccountPersistenceService = get(),
                assetCatalogue = get(),
            )
        }
    }
}
