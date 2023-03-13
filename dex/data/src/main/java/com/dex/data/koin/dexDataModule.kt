package com.dex.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.dex.data.DexAccountsRepository
import com.dex.domain.DexAccountsService
import org.koin.dsl.bind
import org.koin.dsl.module

val dexDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            DexAccountsRepository(
                coincore = get(),
                currencyPrefs = get(),
                exchangeRate = get()
            )
        }.bind(DexAccountsService::class)
    }
}
