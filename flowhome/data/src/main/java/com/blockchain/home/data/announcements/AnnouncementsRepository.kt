package com.blockchain.home.data.announcements

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.home.announcements.Announcement
import com.blockchain.home.announcements.AnnouncementsService
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.Flow

class AnnouncementsRepository(
    private val announcementsStore: AnnouncementsStore
) : AnnouncementsService {

    override fun announcements(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<Announcement>>> {
        return announcementsStore.stream(freshnessStrategy)
            .mapData {
                it.announcements.map {
                    Announcement(
                        id = it.id,
                        title = it.customPayload.title,
                        description = it.customPayload.description,
                        imageUrl = it.customPayload.imageUrl,
                        eligibleModes = listOf(WalletMode.CUSTODIAL),
                        actionUrl = it.customPayload.actionUrl,
                        priority = it.priorityLevel
                    )
                }
            }
    }
}
