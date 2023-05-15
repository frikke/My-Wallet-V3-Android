package com.blockchain.api.announcements

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AnnouncementsApi {
    @GET("api/inApp/getMessages")
    fun getAnnouncements(
        @Header("Api-Key") apiKey: String,
        @Query("email") email: String,
        @Query("count") count: Int,
        @Query("platform") platform: String = "Android",
        @Query("SDKVersion") sdkVersion: String,
        @Query("packageName") packageName: String
    ): Single<AnnouncementsDto>

    @POST("api/events/inAppConsume")
    suspend fun consumeAnnouncement(
        @Header("Api-Key") apiKey: String,
        @Body body: AnnouncementBodyDto
    ): Outcome<Exception, Unit>

    @POST("api/events/trackInAppOpen")
    suspend fun trackSeen(
        @Header("Api-Key") apiKey: String,
        @Body body: AnnouncementBodyDto
    ): Outcome<Exception, Unit>

    @POST("api/events/trackInAppClick")
    suspend fun trackClicked(
        @Header("Api-Key") apiKey: String,
        @Body body: AnnouncementBodyDto
    ): Outcome<Exception, Unit>
}
