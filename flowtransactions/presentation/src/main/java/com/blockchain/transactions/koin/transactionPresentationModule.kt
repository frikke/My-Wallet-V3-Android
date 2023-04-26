package com.blockchain.transactions.koin

import com.blockchain.coincore.CryptoAccount
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.swap.confirmation.ConfirmationViewModel
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.selectsource.SelectSourceViewModel
import com.blockchain.transactions.swap.selecttarget.SelectTargetViewModel
import info.blockchain.balance.CryptoValue
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

        viewModel { (
            sourceAccount: CryptoAccount,
            targetAccount: CryptoAccount,
            sourceCryptoAmount: CryptoValue,
            secondPassword: String?,
        ) ->
            ConfirmationViewModel(
                sourceAccount = sourceAccount,
                targetAccount = targetAccount,
                sourceCryptoAmount = sourceCryptoAmount,
                secondPassword = secondPassword,
                brokerageDataManager = get(),
                exchangeRatesDataManager = get(),
                custodialWalletManager = get(),
                swapTransactionsStore = get(),
                tradingStore = get(),
            )
        }
    }
}
