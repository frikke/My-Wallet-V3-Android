package piuk.blockchain.android.ui.launcher

import android.content.Intent
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.api.Environment
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.PersistentPrefs

interface LauncherView : MvpView {
    fun onCorruptPayload()
    fun getViewIntentData(): ViewIntentData
    fun onNoGuid()
    fun onRequestPin()
    fun onReenterPassword()
}

data class ViewIntentData(
    val action: String?,
    val scheme: String?,
    val dataString: String?,
    val data: String?,
    val isAutomationTesting: Boolean
)

class LauncherPresenter internal constructor(
    private val appUtil: AppUtil,
    private val prefs: PersistentPrefs,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val envSettings: EnvironmentConfig,
    private val authPrefs: AuthPrefs
) : MvpPresenter<LauncherView>() {

    override fun onViewAttached() {
        val viewIntentData = view?.getViewIntentData()

        // Store incoming bitcoin URI if needed
        if (
            viewIntentData?.action == Intent.ACTION_VIEW &&
            viewIntentData.scheme == "bitcoin" &&
            viewIntentData.data != null
        ) {
            prefs.setValue(PersistentPrefs.KEY_SCHEME_URL, viewIntentData.data)
        }
        if (viewIntentData?.action == Intent.ACTION_VIEW && viewIntentData.data != null) {
            deepLinkPersistence.pushDeepLink(viewIntentData.data)
        }

        if (
            Intent.ACTION_VIEW == viewIntentData?.action &&
            viewIntentData.dataString?.contains("blockchain") == true
        ) {
            prefs.setValue(PersistentPrefs.KEY_METADATA_URI, viewIntentData.dataString)
        }

        if (viewIntentData?.isAutomationTesting == true && Environment.STAGING == envSettings.environment) {
            prefs.setIsUnderTest()
        }

        val hasBackup = prefs.hasBackup()
        val hasNoLoginInfo = authPrefs.walletGuid.isEmpty() && prefs.pinId.isEmpty()
        val hasLoggedOut = authPrefs.walletGuid.isNotEmpty() && prefs.pinId.isEmpty()
        val hasFullWalletInfo = authPrefs.walletGuid.isNotEmpty() && prefs.pinId.isNotEmpty() &&
            authPrefs.encryptedPassword.isNotEmpty()

        when {
            hasNoLoginInfo && !hasBackup -> view?.onNoGuid()
            hasNoLoginInfo && hasBackup -> view?.onRequestPin()
            hasLoggedOut -> view?.onReenterPassword()
            hasFullWalletInfo -> view?.onRequestPin()
            !appUtil.isSane -> view?.onCorruptPayload()
            else -> throw IllegalStateException("this state should never happen")
        }
    }

    fun clearCredentialsAndRestart() =
        appUtil.clearCredentialsAndRestart()

    override fun onViewDetached() {}

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = true
}