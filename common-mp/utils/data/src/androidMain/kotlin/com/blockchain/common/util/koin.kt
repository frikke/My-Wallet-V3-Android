package com.blockchain.common.util

import org.koin.dsl.module

val commonMpUtilsModule = module {
    factory<PlatformDeviceIdGenerator<DeviceIdSource>> {
        AndroidDeviceIdGenerator(
            ctx = get()
        )
    }
}
