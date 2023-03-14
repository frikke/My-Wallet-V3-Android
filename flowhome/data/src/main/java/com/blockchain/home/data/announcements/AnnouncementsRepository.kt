package com.blockchain.home.data.announcements

import com.blockchain.api.announcements.AnnouncementPayloadDto
import com.blockchain.api.announcements.ConsumeAnnouncementDto
import com.blockchain.api.announcements.DeviceInfo
import com.blockchain.api.services.AnnouncementsApiService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.doOnData
import com.blockchain.data.map
import com.blockchain.extensions.minus
import com.blockchain.home.announcements.Announcement
import com.blockchain.home.announcements.AnnouncementsService
import com.blockchain.home.announcements.ConsumeAnnouncementAction
import com.blockchain.preferences.IterableAnnouncementsPrefs
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class AnnouncementsRepository(
    private val announcementsStore: AnnouncementsStore,
    private val announcementsApiService: AnnouncementsApiService,
    private val announcementsCredentials: AnnouncementsCredentials,
    private val announcementsPrefs: IterableAnnouncementsPrefs
) : AnnouncementsService {

    private val deletedAnnouncements = MutableStateFlow(
        announcementsPrefs.deletedAnnouncements()
    )

    override fun announcements(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<Announcement>>> {
        return combine(
            deletedAnnouncements,
            announcementsStore.stream(freshnessStrategy)
        ) { deletedAnnouncements, allAnnouncements ->
            allAnnouncements.doOnData {
                // update seen
                it.announcements.filter { it.isRead }
                    .map { it.id }
                    .let { announcementsPrefs.updateSeenAnnouncements(it) }

                // update deleted
                announcementsPrefs.syncDeletedAnnouncements(it.announcements.map { it.id })
            }

            allAnnouncements.map {
                it.announcements.minus {
                    deletedAnnouncements.contains(it.id)
                }
            }
        }.mapData {
            it.map {
                Announcement(
                    id = it.id,
                    title = it.customPayload.title,
                    description = it.customPayload.description,
                    imageUrl = it.customPayload.imageUrl,
                    eligibleModes = it.customPayload.eligibleModes(),
                    actionUrl = it.customPayload.actionUrl,
                    priority = it.priorityLevel
                )
            }
        }
    }

    // todo names might change
    private fun AnnouncementPayloadDto.eligibleModes(): List<WalletMode> {
        return when (appMode) {
            "custodial" -> listOf(WalletMode.CUSTODIAL)
            "defi" -> listOf(WalletMode.NON_CUSTODIAL)
            "universal" -> WalletMode.values().toList()
            else -> emptyList()
        }
    }

    override suspend fun consumeAnnouncement(
        announcement: Announcement,
        action: ConsumeAnnouncementAction
    ) {
        announcementsPrefs.markAsDeleted(announcement.id)
        deletedAnnouncements.value = announcementsPrefs.deletedAnnouncements()

        announcementsApiService.consumeAnnouncement(
            apiKey = announcementsCredentials.apiKey(),
            body = ConsumeAnnouncementDto(
                email = announcementsCredentials.email,
                messageId = announcement.id,
                deleteAction = action.name,
                deviceInfo = DeviceInfo(
                    appPackageName = announcementsCredentials.packageName,
                    deviceId = announcementsCredentials.deviceId,
                    platform = announcementsCredentials.platform
                )
            )
        )
    }
}
