package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.sell.confirmation.SellConfirmationArgs
import com.blockchain.transactions.sell.confirmation.SellConfirmationViewModel
import com.blockchain.transactions.sell.enteramount.SellEnterAmountArgs
import com.blockchain.transactions.sell.enteramount.SellEnterAmountViewModel
import com.blockchain.transactions.sell.sourceaccounts.SellSourceAccountsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val sellTransactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            SellSourceAccountsViewModel(
                sellService = get(),
                assetCatalogue = get()
            )
        }

        viewModel { (args: SellEnterAmountArgs) ->
            SellEnterAmountViewModel(
                args = args,
                analytics = get(),
                sellService = get(),
                tradeDataService = get(),
                assetCatalogue = get(),
                onChainDepositEngineInteractor = get(),
            )
        }

        viewModel { (args: SellConfirmationArgs) ->
            SellConfirmationViewModel(
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
