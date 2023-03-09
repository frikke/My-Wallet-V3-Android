package com.blockchain.home.data.announcements

import com.blockchain.domain.experiments.RemoteConfigService
import kotlinx.coroutines.rx3.await

interface AnnouncementsCredentials {
    suspend fun apiKey(): String
    val email: String
    val count: Int
    val platform: String
    suspend fun sdkVersion(): String
    val packageName: String
    val deviceId: String
}

class AnnouncementsCredentialsImpl internal constructor(
    private val remoteConfigService: RemoteConfigService
) : AnnouncementsCredentials {

    private val apiKey: String? = null
    override suspend fun apiKey(): String {
        return apiKey ?: remoteConfigService.getRawJson(KEY_ITERABLE_API_KEY).await()
    }

    override val email: String
        get() =  "lala@blockchain.com"

    override val count: Int
        get() = 100

    override val platform: String
        get() = "Android"

    override suspend fun sdkVersion(): String {
        return "6.2.17"
    }

    override val packageName: String
        get() = "piuk.blockchain.android.staging"

    override val deviceId: String
        get() = "TODOOOO"

    companion object {
        private const val KEY_ITERABLE_API_KEY = "android_iterable_api_key"
    }
}
