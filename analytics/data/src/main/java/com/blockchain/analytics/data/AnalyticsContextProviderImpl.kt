package com.blockchain.analytics.data

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.accessibility.AccessibilityManager
import com.blockchain.analytics.AnalyticsContextProvider
import com.blockchain.api.analytics.AnalyticsContext
import com.blockchain.api.analytics.DeviceInfo
import com.blockchain.api.analytics.ScreenInfo
import java.util.Locale
import java.util.TimeZone

class AnalyticsContextProviderImpl constructor(
    private val context: Context
) : AnalyticsContextProvider {

    override fun context(): AnalyticsContext {
        return AnalyticsContext(
            device = getDeviceInfo(),
            locale = Locale.getDefault().toString(),
            screen = getScreenInfo(),
            timezone = TimeZone.getDefault().id,
            traits = getTraits()
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

    private fun getTraits() = mapOf(
        "accessibility_enabled" to "${isAccessibilityEnabled()}"
    )

    private fun isAccessibilityEnabled(): Boolean =
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.let {
            it.isTouchExplorationEnabled
        } ?: false
}
