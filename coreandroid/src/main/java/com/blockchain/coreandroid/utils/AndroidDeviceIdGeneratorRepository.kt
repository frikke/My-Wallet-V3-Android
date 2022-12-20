package com.blockchain.coreandroid.utils

import android.annotation.SuppressLint
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.common.util.DeviceIdSource
import com.blockchain.common.util.PlatformDeviceIdGenerator
import com.blockchain.core.utils.DeviceIdGeneratorService

internal class AndroidDeviceIdGeneratorRepository(
    private val platformDeviceIdGenerator: PlatformDeviceIdGenerator<DeviceIdSource>,
    private val analytics: Analytics
) : DeviceIdGeneratorService {

    @SuppressLint("HardwareIds")
    override fun generateId(): String {
        val result = platformDeviceIdGenerator.generateId()

        val analyticsEvent = when (result.deviceIdSource) {
            DeviceIdSource.Android.Uuid -> SOURCE_UUID_GEN
            DeviceIdSource.Android.MacAddress -> SOURCE_MAC_ADDRESS
            DeviceIdSource.Android.AndroidId -> SOURCE_ANDROID_ID
        }
        analytics.logEvent(AnalyticsGenEvent(analyticsEvent))

        return result.deviceId
    }

    private class AnalyticsGenEvent(val source: String) : AnalyticsEvent {
        override val event: String
            get() = EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_PARAM to source)
    }

    companion object {
        const val EVENT_NAME = "generateId"
        const val ANALYTICS_PARAM = "source"
        const val SOURCE_ANDROID_ID = "android_id"
        const val SOURCE_MAC_ADDRESS = "wifi_mac"
        const val SOURCE_UUID_GEN = "uuid_gen"
    }
}
