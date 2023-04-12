package com.dex.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.dex.data.DexAccountsRepository
import com.dex.data.DexAllowanceRepository
import com.dex.data.DexAllowanceStorage
import com.dex.data.DexQuotesRepository
import com.dex.data.DexTransactionRepository
import com.dex.data.stores.DexChainDataStorage
import com.dex.data.stores.DexTokensDataStorage
import com.dex.data.stores.SlippageRepository
import com.dex.domain.AllowanceService
import com.dex.domain.DexAccountsService
import com.dex.domain.DexBalanceService
import com.dex.domain.DexQuotesService
import com.dex.domain.DexTransactionService
import com.dex.domain.SlippageService
import org.koin.dsl.bind
import org.koin.dsl.module

val dexDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            DexAccountsRepository(
                coincore = get(),
                dexTokensDataStorage = get(),
                dexPrefs = get(),
                assetCatalogue = get(),
                currencyPrefs = get()
            )
        }.bind(DexAccountsService::class)

        scoped {
            DexChainDataStorage(
                dexApiService = get()
            )
        }

        scoped {
            DexTokensDataStorage(
                dexApiService = get()
            )
        }

        scoped {
            DexQuotesRepository(
                dexQuotesApiService = get(),
                coincore = get(),
                defiWalletReceiveAddressService = get(),
                assetCatalogue = get()
            )
        }.apply {
            bind(DexQuotesService::class)
            bind(DexBalanceService::class)
        }

        scoped {
            DexAllowanceRepository(
                apiService = get(),
                dexAllowanceStorage = get(),
                gasFeeCalculator = get(),
                assetCatalogue = get(),
                nonCustodialService = get(),
                networkAccountsService = get(),
                defiAccountReceiveAddressService = get()
            )
        }.bind(AllowanceService::class)

        scoped {
            DexTransactionRepository(
                apiService = get(),
                networkAccountsService = get(),
                nonCustodialService = get()
            )
        }.bind(DexTransactionService::class)

        factory {
            SlippageRepository(
                slippagePersistence = get()
            )
        }.bind(SlippageService::class)

        scoped {
            DexAllowanceStorage(
                apiService = get()
            )
        }
    }
}
