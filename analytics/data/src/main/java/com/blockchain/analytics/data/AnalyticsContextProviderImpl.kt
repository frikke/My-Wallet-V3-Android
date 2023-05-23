package com.blockchain.analytics.data

import android.content.res.Resources
import android.os.Build
import com.blockchain.analytics.AnalyticsContext
import com.blockchain.analytics.AnalyticsContextProvider
import com.blockchain.analytics.DeviceInfo
import com.blockchain.analytics.ScreenInfo
import com.blockchain.analytics.TraitsService
import com.blockchain.walletmode.WalletMode
import java.util.Locale
import java.util.TimeZone

class AnalyticsContextProviderImpl constructor(
    private val traitsServices: List<TraitsService>
) : AnalyticsContextProvider {

    override suspend fun context(
        overrideWalletMode: WalletMode?
    ): AnalyticsContext {
        return AnalyticsContext(
            device = getDeviceInfo(),
            locale = Locale.getDefault().toString(),
            screen = getScreenInfo(),
            timezone = TimeZone.getDefault().id,
            traits = traitsServices.map { traitsService -> traitsService.traits(overrideWalletMode) }
                .reduce { acc, map -> acc.plus(map) }
        )
    }

    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            Build.MANUFACTURER,
            Build.MODEL,
            Build.DEVICE
        )
    }

    private fun getScreenInfo(): ScreenInfo {
        return ScreenInfo(
            width = Resources.getSystem().displayMetrics.widthPixels,
            height = Resources.getSystem().displayMetrics.heightPixels,
            density = Resources.getSystem().displayMetrics.density
        )
    }
}
