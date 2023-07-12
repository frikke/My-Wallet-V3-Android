package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.swap.confirmation.FeeExplainerDismissState
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.transactions.swap.confirmation.SwapConfirmationViewModel
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountArgs
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountViewModel
import com.blockchain.transactions.swap.sourceaccounts.SwapSourceAccountsViewModel
import com.blockchain.transactions.swap.targetaccounts.SwapTargetAccountsViewModel
import com.blockchain.transactions.swap.targetassets.SwapTargetAssetsViewModel
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val swapTransactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            SwapSourceAccountsViewModel(
                swapService = get(),
                assetCatalogue = get()
            )
        }

        viewModel { (args: SwapEnterAmountArgs) ->
            SwapEnterAmountViewModel(
                args = args,
                swapService = get(),
                exchangeRates = get(),
                walletModeService = get(),
                tradeDataService = get(),
                assetCatalogue = get(),
                onChainDepositEngineInteractor = get(),
                currencyPrefs = get(),
            )
        }

        viewModel { (args: SwapConfirmationArgs) ->
            SwapConfirmationViewModel(
                args = args,
                swapService = get(),
                brokerageDataManager = get(),
                exchangeRatesDataManager = get(),
                custodialWalletManager = get(),
                swapTransactionsStore = get(),
                tradingStore = get(),
                assetCatalogue = get()
            )
        }

        scoped { FeeExplainerDismissState() }

        viewModel { (sourceTicker: String) ->
            SwapTargetAssetsViewModel(
                sourceTicker = sourceTicker,
                swapService = get(),
                pricesService = get(),
                currencyPrefs = get(),
                walletModeService = get(),
                assetCatalogue = get()
            )
        }

        viewModel { (sourceTicker: String, targetTicker: String, mode: WalletMode) ->
            SwapTargetAccountsViewModel(
                sourceTicker = sourceTicker,
                targetTicker = targetTicker,
                mode = mode,
                swapService = get(),
                assetCatalogue = get()
            )
        }
    }
}
