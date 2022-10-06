package piuk.blockchain.android.ui.launcher

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.SecurityPrefs
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.utils.EncryptedPrefs
import piuk.blockchain.androidcore.utils.SessionPrefs
import piuk.blockchain.androidcore.utils.extensions.isValidGuid
import timber.log.Timber

sealed interface LaunchNavigationEvent : NavigationEvent {
    object AppMaintenance : LaunchNavigationEvent
    object NoGuid : LaunchNavigationEvent
    object RequestPin : LaunchNavigationEvent
    object ReenterPassword : LaunchNavigationEvent
    object CorruptPayload : LaunchNavigationEvent
}

sealed interface LauncherIntent : Intent<LauncherState> {
    object ResumeAppFlow : LauncherIntent
    object ClearCredentialsAndRestart : LauncherIntent
}

@Parcelize
data class LauncherState(
    val action: String? = null,
    val scheme: String? = null,
    val dataString: String? = null,
    val data: String? = null,
    val isAutomationTesting: Boolean = false,
    val referralSuccessTitle: String? = null,
    val referralSuccessBody: String? = null
) : ViewState, ModelState, ModelConfigArgs.ParcelableArgs

class LauncherViewModel internal constructor(
    private val appUtil: AppUtil,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val envSettings: EnvironmentConfig,
    private val authPrefs: AuthPrefs,
    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase,
    private val sessionPrefs: SessionPrefs,
    private val securityPrefs: SecurityPrefs,
    private val referralPrefs: ReferralPrefs,
    private val encryptedPrefs: EncryptedPrefs
) : MviViewModel<LauncherIntent,
    LauncherState,
    LauncherState,
    LaunchNavigationEvent,
    LauncherState>(LauncherState()) {

    override fun viewCreated(args: LauncherState) {

        updateState { args }

        // check app maintenance status
        viewModelScope.launch {
            try {
                getAppMaintenanceConfigUseCase().let { status ->
                    when (status) {
                        AppMaintenanceStatus.NonActionable.Unknown,
                        AppMaintenanceStatus.NonActionable.AllClear -> {
                            extractDataAndStart(modelState)
                        }

                        else -> {
                            navigate(LaunchNavigationEvent.AppMaintenance)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e("Cannot get maintenance config, $e")
                extractDataAndStart(modelState)
            }
        }
    }

    override suspend fun handleIntent(modelState: LauncherState, intent: LauncherIntent) {
        when (intent) {
            LauncherIntent.ResumeAppFlow -> {
                resumeAppFlow()
            }
            LauncherIntent.ClearCredentialsAndRestart -> {
                clearCredentialsAndRestart()
            }
        }
    }

    override fun reduce(state: LauncherState): LauncherState = state

    private fun resumeAppFlow() {
        extractDataAndStart(modelState)
    }

    private fun extractDataAndStart(state: LauncherState) {
        // Store incoming bitcoin URI if needed
        if (
            state.action == android.content.Intent.ACTION_VIEW &&
            state.scheme == "bitcoin" &&
            state.data != null
        ) {
            sessionPrefs.keySchemeUrl = state.data
        }
        if (state.data != null) {
            deepLinkPersistence.pushDeepLink(state.data)
        }

        if (state.referralSuccessBody != null && state.referralSuccessTitle != null) {
            referralPrefs.referralSuccessTitle = state.referralSuccessTitle
            referralPrefs.referralSuccessBody = state.referralSuccessBody
        }

        if (
            android.content.Intent.ACTION_VIEW == state.action &&
            state.dataString?.contains("blockchain") == true
        ) {
            sessionPrefs.metadataUri = state.dataString
        }

        if (state.isAutomationTesting && Environment.STAGING == envSettings.environment) {
            securityPrefs.setIsUnderTest()
        }

        val hasBackup = encryptedPrefs.hasBackup()
        val walletId = authPrefs.walletGuid
        val pinId = authPrefs.pinId

        val isWalletIdInValid = walletId.isNotEmpty() && !walletId.isValidGuid()
        val hasUnPairedWallet = walletId.isNotEmpty() && pinId.isEmpty()
        val hasLoggedIn = walletId.isNotEmpty() && pinId.isNotEmpty()

        when {
            isWalletIdInValid -> {
                navigate(LaunchNavigationEvent.CorruptPayload)
            }
            hasLoggedIn -> {
                navigate(LaunchNavigationEvent.RequestPin)
            }
            hasUnPairedWallet -> {
                navigate(LaunchNavigationEvent.ReenterPassword)
            }
            walletId.isEmpty() -> {
                if (hasBackup) {
                    navigate(LaunchNavigationEvent.RequestPin)
                } else {
                    navigate(LaunchNavigationEvent.NoGuid)
                }
            }
            else -> {
                throw IllegalStateException("Startup is broken - this state should never happen")
            }
        }
    }

    private fun clearCredentialsAndRestart() {
        appUtil.clearCredentialsAndRestart()
    }
}
