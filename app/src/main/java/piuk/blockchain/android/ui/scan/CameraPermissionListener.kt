package piuk.blockchain.android.ui.scan

import com.blockchain.analytics.Analytics
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CameraPermissionListener(
    private val granted: () -> Unit,
    private val denied: () -> Unit = {}
) : BasePermissionListener(), KoinComponent {

    private val analytics: Analytics by inject()

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        granted()
        analytics.logEvent(CameraAnalytics.CameraPermissionRequestActioned(true))
        super.onPermissionGranted(response)
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        denied()
        analytics.logEvent(CameraAnalytics.CameraPermissionRequestActioned(false))
        super.onPermissionDenied(response)
    }
}
