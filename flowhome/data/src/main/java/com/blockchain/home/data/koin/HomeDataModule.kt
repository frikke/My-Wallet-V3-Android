package com.blockchain.home.data.koin

import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.data.FiltersStorage
import com.blockchain.home.data.HomeAccountsRepository
import com.blockchain.home.data.activity.CustodialActivityRepository
import com.blockchain.home.data.activity.dataresource.CustodialActivityStore
import com.blockchain.home.domain.FiltersService
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.superAppModeService
import org.koin.dsl.bind
import org.koin.dsl.module

val homeDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            HomeAccountsRepository(
                unifiedBalancesService = get(),
                coincore = get(),
                walletModeService = get(superAppModeService)
            )
        }.bind(HomeAccountsService::class)

        scoped {
            CustodialActivityStore(coincore = get())
        }

        factory<CustodialActivityService> {
            CustodialActivityRepository(custodialActivityStore = get())
        }
    }

    factory {
        FiltersStorage(sharedPreferences = get())
    }.bind(FiltersService::class)
}
