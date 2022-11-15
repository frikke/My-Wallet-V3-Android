package com.blockchain.unifiedcryptowallet.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.store.KeyedStore
import com.blockchain.store.Store
import com.blockchain.unifiedcryptowallet.data.activity.datasource.UnifiedActivityStore
import com.blockchain.unifiedcryptowallet.data.activity.repository.UnifiedActivityRepository
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesRepository
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesStore
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesSubscribeStore
import com.blockchain.unifiedcryptowallet.data.wallet.NetworkWalletRepository
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletService
import org.koin.dsl.bind
import org.koin.dsl.module

val unifiedCryptoWalletModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            UnifiedBalancesRepository(
                networkAccountsService = get(),
                unifiedBalancesStore = get(),
                unifiedBalancesSubscribeStore = get(),
                assetCatalogue = get(),
                currencyPrefs = get()
            )
        }.bind(UnifiedBalancesService::class)

        scoped {
            UnifiedBalancesSubscribeStore(
                selfCustodyService = get(),
                unifiedBalancesStore = get()
            )
        }

        scoped {
            UnifiedBalancesStore(
                selfCustodyService = get(),
                currencyPrefs = get()
            )
        }.bind(Store::class)

        scoped {
            UnifiedActivityStore(
                selfCustodyService = get(),
                currencyPrefs = get()
            )
        }.bind(KeyedStore::class)

        scoped {
            UnifiedActivityRepository(
                unifiedActivityStore = get(),
                json = get()
            )
        }.bind(UnifiedActivityService::class)

        scoped {
            NetworkWalletRepository(
                networkAccountsService = get(),
                networksService = get()
            )
        }.bind(NetworkWalletService::class)
    }
}
