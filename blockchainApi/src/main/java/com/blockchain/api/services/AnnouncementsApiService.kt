package com.blockchain.api.services

import com.blockchain.api.announcements.AnnouncementsApi
import com.blockchain.api.announcements.AnnouncementsDto
import com.blockchain.api.announcements.AnnouncementBodyDto
import io.reactivex.rxjava3.core.Single

class AnnouncementsApiService internal constructor(
    private val api: AnnouncementsApi
) {
    fun getAnnouncements(
        apiKey: String,
        email: String,
        count: Int,
        platform: String,
        sdkVersion: String,
        packageName: String,
    ): Single<AnnouncementsDto> = api.getAnnouncements(
        apiKey = apiKey,
        email = email,
        count = count,
        platform = platform,
        sdkVersion = sdkVersion,
        packageName = packageName
    )

    suspend fun consumeAnnouncement(
        apiKey: String,
        body: AnnouncementBodyDto,
    ) = api.consumeAnnouncement(
        apiKey = apiKey,
        body = body
    )

   suspend fun seenAnnouncement(
       apiKey: String,
       body: AnnouncementBodyDto,
    ) = api.seenAnnouncement(
        apiKey = apiKey,
        body = body
    )


}
