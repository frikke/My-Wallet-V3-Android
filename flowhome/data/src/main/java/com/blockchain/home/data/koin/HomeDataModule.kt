package com.blockchain.home.data.koin

import com.blockchain.home.data.HomeAccountsRepository
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.bind
import org.koin.dsl.module

val homeDataModule = module {
    scope(payloadScopeQualifier) {
        factory {
            HomeAccountsRepository(coincore = get(), walletModeService = get())
        }.bind(HomeAccountsService::class)
    }
}
