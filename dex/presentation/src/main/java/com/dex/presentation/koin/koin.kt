package com.dex.presentation.koin

import com.blockchain.koin.payloadScopeQualifier
import com.dex.domain.DexTransactionProcessor
import com.dex.presentation.DexEnterAmountViewModel
import com.dex.presentation.DexSelectDestinationAccountViewModel
import com.dex.presentation.DexSourceAccountViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dexPresentation = module {
    scope(payloadScopeQualifier) {
        viewModel {
            DexEnterAmountViewModel(
                currencyPrefs = get(),
                txProcessor = get(),
                dexAccountsService = get(),
                exchangeRatesDataManager = get()
            )
        }
        viewModel {
            DexSourceAccountViewModel(
                dexService = get(),
                transactionProcessor = get()
            )
        }

        viewModel {
            DexSelectDestinationAccountViewModel(
                dexService = get(),
                transactionProcessor = get()
            )
        }

        scoped {
            DexTransactionProcessor(
                dexQuotesService = get()
            )
        }
    }
}
