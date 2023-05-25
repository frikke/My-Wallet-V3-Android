package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.swap.confirmation.ConfirmationViewModel
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.targetaccounts.TargetAccountsViewModel
import com.blockchain.transactions.swap.targetassets.TargetAssetsViewModel
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val swapTransactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            EnterAmountViewModel(
                swapService = get(),
                exchangeRates = get(),
                walletModeService = get(),
                tradeDataService = get(),
                onChainDepositEngineInteractor = get(),
                fiatCurrenciesService = get(),
            )
        }

        viewModel { (args: SwapConfirmationArgs) ->
            ConfirmationViewModel(
                args = args,
                brokerageDataManager = get(),
                exchangeRatesDataManager = get(),
                custodialWalletManager = get(),
                swapTransactionsStore = get(),
                tradingStore = get(),
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
    }
}
