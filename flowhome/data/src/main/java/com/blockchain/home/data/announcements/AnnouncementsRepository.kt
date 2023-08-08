package com.blockchain.home.data.announcements

import com.blockchain.api.announcements.AnnouncementBodyDto
import com.blockchain.api.announcements.AnnouncementPayloadDto
import com.blockchain.api.services.AnnouncementsApiService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.doOnData
import com.blockchain.data.map
import com.blockchain.data.mapData
import com.blockchain.extensions.minus
import com.blockchain.home.announcements.Announcement
import com.blockchain.home.announcements.AnnouncementsService
import com.blockchain.home.announcements.ConsumeAnnouncementAction
import com.blockchain.preferences.IterableAnnouncementsPrefs
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
                    createdAt = it.createdAt,
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
            body = AnnouncementBodyDto.consume(
                email = announcementsCredentials.email(),
                messageId = announcement.id,
                deleteAction = action.name,
                deviceInfo = announcementsCredentials.deviceInfo
            )
        )
    }

    override suspend fun trackSeen(
        announcement: Announcement
    ) {
        if (!announcementsPrefs.seenAnnouncements().contains(announcement.id)) {
            announcementsPrefs.markAsSeen(announcement.id)

            announcementsApiService.trackSeen(
                apiKey = announcementsCredentials.apiKey(),
                body = AnnouncementBodyDto.seen(
                    email = announcementsCredentials.email(),
                    messageId = announcement.id,
                    deviceInfo = announcementsCredentials.deviceInfo
                )
            )
        }
    }

    override suspend fun trackClicked(announcement: Announcement) {
        announcementsApiService.trackClicked(
            apiKey = announcementsCredentials.apiKey(),
            body = AnnouncementBodyDto.click(
                email = announcementsCredentials.email(),
                messageId = announcement.id,
                clickedUrl = announcement.actionUrl,
                deviceInfo = announcementsCredentials.deviceInfo
            )
        )
    }
}
