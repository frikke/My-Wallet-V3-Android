package piuk.blockchain.android.ui.scan

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import java.io.Serializable

sealed class CameraAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class CameraPermissionChecked(isAuthorised: Boolean) : CameraAnalytics(
        event = AnalyticsNames.CAMERA_PERMISSION_CHECKED.eventName,
        params = mapOf(
            "is_authorised" to isAuthorised
        )
    )

    class CameraPermissionRequestActioned(approved: Boolean) : CameraAnalytics(
        event = AnalyticsNames.CAMERA_PERMISSION_REQUESTED.eventName,
        params = mapOf(
            "action" to if (approved) CameraRequestAction.ALLOW.name else CameraRequestAction.REJECT.name
        )
    )

    class QrCodeClicked(
        override val origin: LaunchOrigin = LaunchOrigin.DASHBOARD
    ) : CameraAnalytics(
        event = AnalyticsNames.QR_CODE_CLICKED.eventName,
    )

    class QrCodeScanned(type: QrCodeType) : CameraAnalytics(
        event = AnalyticsNames.QR_CODE_SCANNED.eventName,
        params = mapOf(
            "qr_type" to type.toString()
        )
    )

    private enum class CameraRequestAction {
        ALLOW, REJECT
    }
}

enum class QrCodeType {
    CRYPTO_ADDRESS, DAPP, DEEPLINK, INVALID, LOG_IN
}
