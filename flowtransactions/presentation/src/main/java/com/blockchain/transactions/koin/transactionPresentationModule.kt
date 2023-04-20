package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.selectsource.SelectSourceViewModel
import com.blockchain.transactions.swap.selecttarget.SelectTargetViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val transactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            EnterAmountViewModel(
                swapService = get(),
                exchangeRates = get(),
                currencyPrefs = get()
            )
        }

        viewModel {
            SelectSourceViewModel(
                swapService = get(),
                assetCatalogue = get()
            )
        }

        viewModel { (sourceTicker: String) ->
            SelectTargetViewModel(
                sourceTicker = sourceTicker,
                swapService = get(),
                pricesService = get(),
                currencyPrefs = get()
            )
        }
    }
}
