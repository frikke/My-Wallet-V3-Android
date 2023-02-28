package com.blockchain.api.announcements

import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AnnouncementsApi {
    @GET("api/inApp/getMessages")
    fun getAnnouncements(
        @Header("Api-Key") apiKey: String,

        @Query("email") email: String,
        @Query("count") count: String,
        @Query("platform") platform: String = "Android",
        @Query("SDKVersion") sdkVersion: String,
        @Query("packageName") packageName: String,
    ): Single<AnnouncementsDto>
}