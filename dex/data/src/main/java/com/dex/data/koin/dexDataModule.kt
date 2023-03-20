package com.dex.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.dex.data.DexAccountsRepository
import com.dex.data.stores.DexChainDataStorage
import com.dex.data.stores.DexTokensDataStorage
import com.dex.domain.DexAccountsService
import org.koin.dsl.bind
import org.koin.dsl.module

val dexDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            DexAccountsRepository(
                coincore = get(),
                dexChainDataStorage = get(),
                dexTokensDataStorage = get(),
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
    }
}
