package com.blockchain.home.data.announcements

import com.blockchain.api.announcements.AnnouncementsDto
import com.blockchain.api.services.AnnouncementsApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource

class AnnouncementsStore(
    private val announcementsApiService: AnnouncementsApiService,
    private val announcementsCredentials: AnnouncementsCredentials
) : Store<AnnouncementsDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = "AnnouncementsStore",
        fetcher = Fetcher.Keyed.ofSingle {
            announcementsApiService.getAnnouncements(
                apiKey = announcementsCredentials.apiKey(),
                email = announcementsCredentials.email(),
                count = announcementsCredentials.count,
                platform = announcementsCredentials.platform,
                sdkVersion = announcementsCredentials.sdkVersion(),
                packageName = announcementsCredentials.packageName
            )
        },
        dataSerializer = AnnouncementsDto.serializer(),
        mediator = FreshnessMediator(Freshness.ofHours(24))
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }
}
