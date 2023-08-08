package com.dex.presentation.koin

import com.blockchain.koin.payloadScopeQualifier
import com.dex.domain.AllowanceTransactionProcessor
import com.dex.domain.DexTransactionProcessor
import com.dex.presentation.DexSelectDestinationAccountViewModel
import com.dex.presentation.DexSourceAccountViewModel
import com.dex.presentation.SettingsViewModel
import com.dex.presentation.TokenAllowanceViewModel
import com.dex.presentation.confirmation.DexConfirmationViewModel
import com.dex.presentation.enteramount.DexEnterAmountViewModel
import com.dex.presentation.inprogress.DexInProgressTxViewModel
import com.dex.presentation.network.SelectNetworkViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dexPresentation = module {
    scope(payloadScopeQualifier) {
        viewModel {
            DexEnterAmountViewModel(
                currencyPrefs = get(),
                txProcessor = get(),
                enviromentConfig = get(),
                dexAllowanceService = get(),
                dexAccountsService = get(),
                exchangeRatesDataManager = get(),
                allowanceProcessor = get(),
                dexSlippageService = get(),
                dexNetworkService = get(),
                assetCatalogue = get(),
                dexEligibilityService = get(),
                oneTimeAccountPersistenceService = get()
            )
        }

        viewModel {
            DexConfirmationViewModel(
                transactionProcessor = get(),
                exchangeRatesDataManager = get(),
                currencyPrefs = get()
            )
        }

        viewModel {
            DexInProgressTxViewModel(
                txProcessor = get()
            )
        }

        viewModel {
            DexSourceAccountViewModel(
                dexService = get(),
                transactionProcessor = get(),
                dexNetworkService = get()
            )
        }

        viewModel {
            DexSelectDestinationAccountViewModel(
                dexService = get(),
                transactionProcessor = get(),
                dexNetworkService = get()
            )
        }
        viewModel {
            SettingsViewModel(
                slippageService = get(),
                txProcessor = get()
            )
        }

        viewModel {
            TokenAllowanceViewModel(
                assetCatalogue = get(),
                coincore = get()
            )
        }

        viewModel {
            SelectNetworkViewModel(
                dexNetworkService = get(),
                assetCatalogue = get()
            )
        }

        scoped {
            DexTransactionProcessor(
                dexQuotesService = get(),
                allowanceService = get(),
                evmNetworkSigner = get(),
                unifiedActivityService = get(),
                dexTransactionService = get(),
                balanceService = get(),
                notificationTransmitter = get()
            )
        }

        factory {
            AllowanceTransactionProcessor(
                allowanceService = get(),
                evmNetworkSigner = get(),
                notificationTransmitter = get()
            )
        }
    }
}
