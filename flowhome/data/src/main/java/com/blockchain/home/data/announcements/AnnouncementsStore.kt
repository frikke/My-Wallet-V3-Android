package com.blockchain.home.data.announcements

import com.blockchain.api.announcements.AnnouncementsApi
import com.blockchain.api.announcements.AnnouncementsDto
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.rx3.await

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
