package com.blockchain.common.util

interface PlatformDeviceIdGenerator<out PlatformIdSource : DeviceIdSource> {
    fun generateId(): PlatformDeviceId<out PlatformIdSource>
}

sealed interface DeviceIdSource {
    sealed class Android : DeviceIdSource {
        object Uuid : DeviceIdSource.Android()
        object MacAddress : DeviceIdSource.Android()
        object AndroidId : DeviceIdSource.Android()
    }
}

data class PlatformDeviceId<Type : DeviceIdSource>(val deviceId: String, val deviceIdSource: Type)
