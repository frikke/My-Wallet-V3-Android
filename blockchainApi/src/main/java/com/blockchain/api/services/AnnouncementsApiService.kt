package com.blockchain.api.services

import com.blockchain.api.announcements.AnnouncementBodyDto
import com.blockchain.api.announcements.AnnouncementsApi
import com.blockchain.api.announcements.AnnouncementsDto
import com.blockchain.outcome.getOrDefault
import io.reactivex.rxjava3.core.Single

class AnnouncementsApiService internal constructor(
    private val api: AnnouncementsApi
) {
    fun getAnnouncements(
        apiKey: String,
        email: String,
        count: Int,
        platform: String,
        packageName: String
    ): Single<AnnouncementsDto> = api.getAnnouncements(
        apiKey = apiKey,
        email = email,
        count = count,
        platform = platform,
        packageName = packageName
    )

    suspend fun consumeAnnouncement(
        apiKey: String,
        body: AnnouncementBodyDto
    ) = api.consumeAnnouncement(
        apiKey = apiKey,
        body = body
    ).getOrDefault(Unit)

    suspend fun trackSeen(
        apiKey: String,
        body: AnnouncementBodyDto
    ) = api.trackSeen(
        apiKey = apiKey,
        body = body
    ).getOrDefault(Unit)

    suspend fun trackClicked(
        apiKey: String,
        body: AnnouncementBodyDto
    ) = api.trackClicked(
        apiKey = apiKey,
        body = body
    ).getOrDefault(Unit)
}
