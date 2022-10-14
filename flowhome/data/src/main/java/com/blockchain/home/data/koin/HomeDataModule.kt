package com.blockchain.home.data.koin

import com.blockchain.home.data.HomeAccountsRepository
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.superAppModeService
import org.koin.dsl.bind
import org.koin.dsl.module

val homeDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            HomeAccountsRepository(
                coincore = get(),
                walletModeService = get(superAppModeService)
            )
        }.bind(HomeAccountsService::class)
    }
}
