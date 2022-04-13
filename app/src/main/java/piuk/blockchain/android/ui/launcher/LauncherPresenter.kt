package piuk.blockchain.android.ui.launcher

import android.content.Intent
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.preferences.AuthPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.isValidGuid
import kotlin.coroutines.CoroutineContext

interface LauncherView : MvpView {
    fun onAppMaintenance()
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
    private val authPrefs: AuthPrefs,
    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase
) : MvpPresenter<LauncherView>(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext = job + Dispatchers.IO

    override fun onViewAttached() {
        launch {
            // check app maintenance status
            getAppMaintenanceConfigUseCase().let { status ->
                when (status) {
                    AppMaintenanceStatus.NonActionable.Unknown,
                    AppMaintenanceStatus.NonActionable.AllClear -> {
                        kickOff()
                    }

                    else -> {
                        view?.onAppMaintenance()
                    }
                }
            }
        }
    }

    fun resumeAppFlow() {
        kickOff()
    }

    private fun kickOff() {
        val viewIntentData = view?.getViewIntentData()

        // Store incoming bitcoin URI if needed
        if (
            viewIntentData?.action == Intent.ACTION_VIEW &&
            viewIntentData.scheme == "bitcoin" &&
            viewIntentData.data != null
        ) {
            prefs.setValue(PersistentPrefs.KEY_SCHEME_URL, viewIntentData.data)
        }
        if (viewIntentData?.data != null) {
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
        val walletId = authPrefs.walletGuid
        val pinId = prefs.pinId

        val isWalletIdInValid = walletId.isNotEmpty() && !walletId.isValidGuid()
        val hasUnPairedWallet = walletId.isNotEmpty() && pinId.isEmpty()
        val hasLoggedIn = walletId.isNotEmpty() && pinId.isNotEmpty()

        when {
            isWalletIdInValid -> view?.onCorruptPayload()
            hasLoggedIn -> view?.onRequestPin()
            hasUnPairedWallet -> view?.onReenterPassword()
            walletId.isEmpty() -> if (hasBackup) view?.onRequestPin() else view?.onNoGuid()
            else -> throw IllegalStateException("Startup is broken - this state should never happen")
        }
    }

    fun clearCredentialsAndRestart() =
        appUtil.clearCredentialsAndRestart()

    override fun onViewDetached() {
        job.cancel()
    }

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = true
}
