package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.common.OnChainDepositEngineInteractor
import com.blockchain.transactions.common.sourceaccounts.SourceAccountsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val commonTransactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        factory {
            OnChainDepositEngineInteractor(
                custodialWalletManager = get(),
                exchangeRatesDataManager = get(),
            )
        }

        viewModel { (isSwap: Boolean) ->
            SourceAccountsViewModel(
                isSwap = isSwap,
                sellService = get(),
                swapService = get(),
                assetCatalogue = get()
            )
        }
    }
}
