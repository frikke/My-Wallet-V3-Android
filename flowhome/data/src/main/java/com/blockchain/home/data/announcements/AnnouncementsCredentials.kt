package com.blockchain.home.data.announcements

import com.blockchain.api.announcements.DeviceInfo
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.outcome.getOrDefault
import com.blockchain.store.mapData
import com.blockchain.utils.awaitOutcome
import kotlinx.coroutines.flow.firstOrNull

interface AnnouncementsCredentials {
    suspend fun apiKey(): String
    suspend fun email(): String
    val count: Int
    val platform: String
    suspend fun sdkVersion(): String
    val packageName: String
    val deviceId: String
    val deviceInfo: DeviceInfo
}

class AnnouncementsCredentialsImpl internal constructor(
    private val remoteConfigService: RemoteConfigService,
    private val userService: UserService,
    private val environmentConfig: EnvironmentConfig
) : AnnouncementsCredentials {

    private val apiKey: String? = null
    override suspend fun apiKey(): String {
        return apiKey ?: remoteConfigService.getRawJson(KEY_ITERABLE_API_KEY).awaitOutcome().getOrDefault("")
    }

    override suspend fun email(): String {
        return userService.getUserResourceFlow(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .mapData { it.email }
            .firstOrNull()
            ?.dataOrElse(null).orEmpty()
    }

    override val count: Int
        get() = 100

    override val platform: String
        get() = "Android"

    override suspend fun sdkVersion(): String {
        return "6.2.17"
    }

    override val packageName: String
        get() = environmentConfig.applicationId

    override val deviceId: String
        get() = "TODOOOO"

    override val deviceInfo: DeviceInfo
        get() = DeviceInfo(
            appPackageName = packageName,
            deviceId = deviceId,
            platform = platform
        )

    companion object {
        private const val KEY_ITERABLE_API_KEY = "android_iterable_api_key"
    }
}
