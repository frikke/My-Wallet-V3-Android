package piuk.blockchain.android.ui.launcher

import android.net.Uri
import androidx.annotation.StringRes
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom

interface LauncherView : View {

    fun getViewIntentData(): ViewIntentData

    fun onNoGuid()

    fun onRequestPin()

    fun onCorruptPayload()

    fun onStartMainActivity(uri: Uri?, launchBuySellIntro: Boolean = false)

    fun launchEmailVerification()

    fun onReEnterPassword()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showMetadataNodeFailure()

    fun showSecondPasswordDialog()

    fun updateProgressVisibility(show: Boolean)
}

data class ViewIntentData(
    val action: String?,
    val scheme: String?,
    val dataString: String?,
    val data: String?,
    val isAfterWalletCreation: Boolean,
    val isPinValidated: Boolean,
    val isAutomationTesting: Boolean
)