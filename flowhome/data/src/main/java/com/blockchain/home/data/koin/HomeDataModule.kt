package com.blockchain.home.data.koin

import com.blockchain.home.actions.QuickActionsService
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.announcements.AnnouncementsService
import com.blockchain.home.data.FiltersStorage
import com.blockchain.home.data.HomeAccountsRepository
import com.blockchain.home.data.actions.QuickActionsRepository
import com.blockchain.home.data.activity.CustodialActivityRepository
import com.blockchain.home.data.announcements.AnnouncementsRepository
import com.blockchain.home.data.announcements.AnnouncementsStore
import com.blockchain.home.data.emptystate.CustodialEmptyCardRepository
import com.blockchain.home.data.emptystate.EmptyStateBuyAmountsRemoteConfig
import com.blockchain.home.domain.FiltersService
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.emptystate.CustodialEmptyCardService
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.bind
import org.koin.dsl.module

val homeDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            HomeAccountsRepository(
                unifiedBalancesService = get(),
                coincore = get()
            )
        }.bind(HomeAccountsService::class)

        scoped<CustodialActivityService> {
            CustodialActivityRepository(coincore = get())
        }

        scoped {
            QuickActionsRepository(
                coincore = get(),
                userFeaturePermissionService = get()
            )
        }.bind(QuickActionsService::class)

        scoped {
            CustodialEmptyCardRepository(
                emptyStateBuyAmountsRemoteConfig = get()
            )
        }.bind(CustodialEmptyCardService::class)

        scoped {
            EmptyStateBuyAmountsRemoteConfig(
                remoteConfigService = get(),
                json = get()
            )
        }

        scoped {
            AnnouncementsRepository(
                announcementsStore = get()
            )
        }.bind(AnnouncementsService::class)

        scoped {
            AnnouncementsStore(
                announcementsApi = get(),
                remoteConfigService = get()
            )
        }
    }

    factory {
        FiltersStorage(smallBalancesPrefs = get())
    }.bind(FiltersService::class)
}
