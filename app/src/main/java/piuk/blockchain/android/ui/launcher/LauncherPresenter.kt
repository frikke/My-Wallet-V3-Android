package piuk.blockchain.android.ui.launcher

import android.content.Intent
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.SecurityPrefs
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.utils.EncryptedPrefs
import piuk.blockchain.androidcore.utils.SessionPrefs
import piuk.blockchain.androidcore.utils.extensions.isValidGuid
import timber.log.Timber

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
    val isAutomationTesting: Boolean,
    val referralSuccessTitle: String?,
    val referralSuccessBody: String?
)

class LauncherPresenter internal constructor(
    private val appUtil: AppUtil,
    private val prefs: SessionPrefs,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val envSettings: EnvironmentConfig,
    private val authPrefs: AuthPrefs,
    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase,
    private val appMaintenanceFF: FeatureFlag,
    private val securityPrefs: SecurityPrefs,
    private val referralPrefs: ReferralPrefs,
    private val encryptedPrefs: EncryptedPrefs
) : MvpPresenter<LauncherView>() {

    override fun onViewCreated() {
        appMaintenanceFF.enabled.subscribe { enabled ->
            if (enabled) {
                // check app maintenance status
                rxSingle { getAppMaintenanceConfigUseCase() }.subscribeBy(
                    onSuccess = { status ->
                        when (status) {
                            AppMaintenanceStatus.NonActionable.Unknown,
                            AppMaintenanceStatus.NonActionable.AllClear -> {
                                extractDataAndStart()
                            }

                            else -> {
                                view?.onAppMaintenance()
                            }
                        }
                    },
                    onError = {
                        Timber.e("Cannot get maintenance config, $it")
                        extractDataAndStart()
                    }
                )
            }
        }
    }

    override fun onViewAttached() {
        appMaintenanceFF.enabled.subscribe { enabled ->
            if (!enabled) {
                extractDataAndStart()
            }
        }
    }

    fun resumeAppFlow() {
        extractDataAndStart()
    }

    private fun extractDataAndStart() {
        val viewIntentData = view?.getViewIntentData()

        // Store incoming bitcoin URI if needed
        if (
            viewIntentData?.action == Intent.ACTION_VIEW &&
            viewIntentData.scheme == "bitcoin" &&
            viewIntentData.data != null
        ) {
            prefs.keySchemeUrl = viewIntentData.data
        }
        if (viewIntentData?.data != null) {
            deepLinkPersistence.pushDeepLink(viewIntentData.data)
        }

        if (viewIntentData?.referralSuccessBody != null && viewIntentData.referralSuccessTitle != null) {
            referralPrefs.referralSuccessTitle = viewIntentData.referralSuccessTitle
            referralPrefs.referralSuccessBody = viewIntentData.referralSuccessBody
        }

        if (
            Intent.ACTION_VIEW == viewIntentData?.action &&
            viewIntentData.dataString?.contains("blockchain") == true
        ) {
            prefs.metadataUri =  viewIntentData.dataString
        }

        if (viewIntentData?.isAutomationTesting == true && Environment.STAGING == envSettings.environment) {
            securityPrefs.setIsUnderTest()
        }

        val hasBackup = encryptedPrefs.hasBackup()
        val walletId = authPrefs.walletGuid
        val pinId = authPrefs.pinId

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
    }

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = true
}
