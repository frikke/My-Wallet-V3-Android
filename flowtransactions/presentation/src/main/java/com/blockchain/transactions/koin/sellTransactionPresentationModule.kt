package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.sell.confirmation.ConfirmationViewModel
import com.blockchain.transactions.sell.confirmation.SellConfirmationArgs
import com.blockchain.transactions.sell.enteramount.EnterAmountViewModel
import com.blockchain.transactions.sell.sourceaccounts.SourceAccountsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val sellTransactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            SourceAccountsViewModel(
                sellService = get(),
                assetCatalogue = get()
            )
        }

        viewModel {
            EnterAmountViewModel(
                analytics = get(),
                sellService = get(),
                tradeDataService = get(),
                onChainDepositEngineInteractor = get(),
            )
        }

        viewModel { (args: SellConfirmationArgs) ->
            ConfirmationViewModel(
                args = args,
                brokerageDataManager = get(),
                exchangeRatesDataManager = get(),
                custodialWalletManager = get(),
                tradingStore = get(),
                swapActivityStore = get(),
                transactionsStore = get()
            )
        }
    }
}
