package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.swap.confirmation.ConfirmationViewModel
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.sourceaccounts.SourceAccountsViewModel
import com.blockchain.transactions.swap.targetaccounts.TargetAccountsViewModel
import com.blockchain.transactions.swap.targetassets.TargetAssetsViewModel
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val transactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            EnterAmountViewModel(
                swapService = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                walletModeService = get(),
                confirmationArgs = get(),
            )
        }

        viewModel {
            SourceAccountsViewModel(
                swapService = get(),
                assetCatalogue = get()
            )
        }

        viewModel { (sourceTicker: String) ->
            TargetAssetsViewModel(
                sourceTicker = sourceTicker,
                swapService = get(),
                pricesService = get(),
                currencyPrefs = get(),
                walletModeService = get()
            )
        }

        viewModel { (sourceTicker: String, targetTicker: String, mode: WalletMode) ->
            TargetAccountsViewModel(
                sourceTicker = sourceTicker,
                targetTicker = targetTicker,
                mode = mode,
                swapService = get(),
                assetCatalogue = get()
            )
        }

        scoped {
            SwapConfirmationArgs()
        }

        viewModel {
            ConfirmationViewModel(
                confirmationArgs = get(),
                brokerageDataManager = get(),
                exchangeRatesDataManager = get(),
                custodialWalletManager = get(),
                swapTransactionsStore = get(),
                tradingStore = get(),
            )
        }
    }
}
