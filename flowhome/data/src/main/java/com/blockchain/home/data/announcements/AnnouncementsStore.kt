package com.blockchain.home.data.announcements

import com.blockchain.api.announcements.AnnouncementsApi
import com.blockchain.api.announcements.AnnouncementsDto
import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.NabuUserReporter
import com.blockchain.nabu.datamanagers.WalletReporter
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.builtins.ListSerializer
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AnnouncementsStore(
    private val announcementsApi: AnnouncementsApi,
    private val remoteConfigService: RemoteConfigService
) : Store<AnnouncementsDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = "AnnouncementsStore",
        fetcher = Fetcher.Keyed.ofSingle {
            val apiKey = remoteConfigService.getRawJson(KEY_ITERABLE_API_KEY).await()
            announcementsApi.getAnnouncements(
                apiKey = apiKey,
                email = "lala@blockchain.com",
                count = "123",
                platform = "Android",
                sdkVersion = "6.2.17",
                packageName = "piuk.blockchain.android.staging"
            )
        },
        dataSerializer = AnnouncementsDto.serializer(),
        mediator = FreshnessMediator(Freshness.ofHours(24))
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val KEY_ITERABLE_API_KEY = "android_iterable_api_key"
    }
}
