package piuk.blockchain.android.util

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.extensions.filterNotNullValues
import java.io.Serializable

sealed class AppAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class AppInstalled(
        versionCode: Int,
        versionName: String,
        installReferrer: String,
        installBeginSeconds: Long,
        adId: String
    ) : AppAnalytics(
        AnalyticsNames.APP_INSTALLED.eventName,
        mapOf(
            BUILD to versionCode,
            VERSION to versionName,
            INSTALL_REFERRER to installReferrer,
            INSTALL_TIMESTAMP to installBeginSeconds,
            AD_ID to adId
        )
    )

    class AppUpdated(
        previousVersionCode: Int?,
        installedVersion: String,
        currentVersionName: String,
        currentVersionCode: Int
    ) : AppAnalytics(
        AnalyticsNames.APP_UPDATED.eventName,
        mapOf(
            PREVIOUS_BUILD to previousVersionCode?.toString(),
            INSTALLED_VERSION to installedVersion,
            BUILD to currentVersionName,
            VERSION to currentVersionCode.toString()
        ).filterNotNullValues()
    )

    object AppBackgrounded : AppAnalytics(AnalyticsNames.APP_BACKGROUNDED.eventName)
    object AppDeepLinked : AppAnalytics(AnalyticsNames.APP_DEEP_LINK_OPENED.eventName)

    companion object {
        private const val BUILD = "build"
        private const val INSTALL_REFERRER = "install_referrer"
        private const val INSTALL_TIMESTAMP = "install_begin_timestamp_server_seconds"
        private const val AD_ID = "gps_adid"
        private const val PREVIOUS_BUILD = "previous_build"
        private const val INSTALLED_VERSION = "installed_version"
        private const val VERSION = "version"
    }
}
