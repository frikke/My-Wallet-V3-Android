package com.blockchain.unifiedcryptowallet.data.koin

import com.blockchain.api.services.ActivityCacheService
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.store.KeyedStore
import com.blockchain.store.Store
import com.blockchain.unifiedcryptowallet.data.Database
import com.blockchain.unifiedcryptowallet.data.activity.datasource.ActivityDetailsStore
import com.blockchain.unifiedcryptowallet.data.activity.datasource.UnifiedActivityCache
import com.blockchain.unifiedcryptowallet.data.activity.repository.UnifiedActivityRepository
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesRepository
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesStore
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesSubscribeStore
import com.blockchain.unifiedcryptowallet.data.wallet.NetworkWalletRepository
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletService
import com.squareup.sqldelight.android.AndroidSqliteDriver
import org.koin.android.ext.koin.androidContext
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
                currencyPrefs = get(),
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
            ActivityDetailsStore(
                selfCustodyService = get(),
                currencyPrefs = get()
            )
        }.bind(KeyedStore::class)

        scoped {
            UnifiedActivityRepository(
                activityWebSocketService = get(),
                activityCache = get(),
                json = get(),
                currencyPrefs = get(),
                activityDetailsStore = get()
            )
        }.bind(UnifiedActivityService::class)

        scoped {
            Database(AndroidSqliteDriver(Database.Schema, androidContext(), "activity_persister.db"))
        }

        scoped {
            get<Database>().activityQueries
        }

        scoped {
            UnifiedActivityCache(
                activityQueries = get(),
                json = get()
            )
        }.bind(ActivityCacheService::class)

        scoped {
            NetworkWalletRepository(
                networkAccountsService = get(),
                networksService = get()
            )
        }.bind(NetworkWalletService::class)
    }
}
