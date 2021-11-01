package piuk.blockchain.android.util

import com.blockchain.extensions.withoutNullValues
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

sealed class AppAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class AppInstalled(
        versionCode: Int,
        versionName: String
    ) : AppAnalytics(
        AnalyticsNames.APP_INSTALLED.eventName, mapOf(
            BUILD to versionCode,
            VERSION to versionName
        )
    )

    class AppUpdated(
        previousVersionCode: Int?,
        installedVersion: String,
        currentVersionName: String,
        currentVersionCode: Int
    ) : AppAnalytics(
        AnalyticsNames.APP_UPDATED.eventName, mapOf(
            PREVIOUS_BUILD to previousVersionCode?.toString(),
            INSTALLED_VERSION to installedVersion,
            BUILD to currentVersionName,
            VERSION to currentVersionCode.toString()
        ).withoutNullValues()
    )

    object AppBackgrounded : AppAnalytics(AnalyticsNames.APP_BACKGROUNDED.eventName)
    object AppDeepLinked : AppAnalytics(AnalyticsNames.APP_DEEP_LINK_OPENED.eventName)

    companion object {
        private const val BUILD = "build"
        private const val PREVIOUS_BUILD = "previous_build"
        private const val INSTALLED_VERSION = "installed_version"
        private const val VERSION = "version"
    }
}