package com.dex.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.dex.data.DexAccountsRepository
import com.dex.data.DexQuotesRepository
import com.dex.data.stores.DexChainDataStorage
import com.dex.data.stores.DexTokensDataStorage
import com.dex.data.stores.SlippageRepository
import com.dex.domain.DexAccountsService
import com.dex.domain.DexBalanceService
import com.dex.domain.DexQuotesService
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
                assetCatalogue = get()
            )
        }.apply {
            bind(DexQuotesService::class)
            bind(DexBalanceService::class)
        }

        factory {
            SlippageRepository(
                slippagePersistence = get()
            )
        }.bind(SlippageService::class)
    }
}
