package piuk.blockchain.android.ui.settings.security

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class SecurityModel(
    initialState: SecurityState,
    mainScheduler: Scheduler,
    private val interactor: SecurityInteractor,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
) : MviModel<SecurityState, SecurityIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(
        previousState: SecurityState,
        intent: SecurityIntent,
    ): Disposable? =
        when (intent) {
            is SecurityIntent.LoadInitialInformation -> interactor.loadInitialInformation()
                .subscribeBy(
                    onSuccess = { securityInfo ->
                        process(SecurityIntent.UpdateSecurityInfo(securityInfo))
                    },
                    onError = {
                        Timber.e("Error loading security info: ${it.message}")
                        process(SecurityIntent.UpdateErrorState(SecurityError.LOAD_INITIAL_INFO_FAIL))
                    }
                )
            is SecurityIntent.ToggleBiometrics -> {
                if (previousState.securityInfo?.isBiometricsEnabled == true) {
                    process(SecurityIntent.UpdateViewState(SecurityViewState.ConfirmBiometricsDisabling))
                } else {
                    process(interactor.checkBiometricsState())
                }
                null
            }
            is SecurityIntent.DisableBiometrics -> {
                interactor.disableBiometricLogin()
                    .subscribeBy(
                        onComplete = {
                            process(SecurityIntent.BiometricsDisabled)
                        },
                        onError = {
                            process(SecurityIntent.UpdateErrorState(SecurityError.BIOMETRICS_DISABLING_FAIL))
                        }
                    )
            }
            is SecurityIntent.ToggleTwoFa -> {
                interactor.checkTwoFaState()
                    .subscribeBy(
                        onSuccess = {
                            process(it)
                        },
                        onError = {
                            process(SecurityIntent.UpdateErrorState(SecurityError.TWO_FA_TOGGLE_FAIL))
                        }
                    )
            }
            is SecurityIntent.EnableTwoFa -> {
                interactor.enableTwoFa()
                    .subscribeBy(
                        onSuccess = {
                            process(SecurityIntent.TwoFactorEnabled)
                        },
                        onError = {
                            process(SecurityIntent.UpdateErrorState(SecurityError.TWO_FA_TOGGLE_FAIL))
                        }
                    )
            }
            is SecurityIntent.ToggleScreenshots -> {
                previousState.securityInfo?.areScreenshotsEnabled?.let { currentSetting ->
                    interactor.updateScreenshotsEnabled(!currentSetting)
                        .subscribeBy(
                            onComplete = {
                                process(SecurityIntent.UpdateScreenshotsEnabled(!currentSetting))
                            },
                            onError = {
                                process(SecurityIntent.UpdateErrorState(SecurityError.SCREENSHOT_UPDATE_FAIL))
                            }
                        )
                    null
                }
            }
            is SecurityIntent.ToggleTor -> {
                previousState.securityInfo?.isTorFilteringEnabled?.let { filteringEnabled ->
                    interactor.updateTor(!filteringEnabled)
                        .subscribeBy(
                            onSuccess = {
                                process(SecurityIntent.UpdateTorFiltering(!filteringEnabled))
                            },
                            onError = {
                                process(SecurityIntent.UpdateErrorState(SecurityError.TOR_FILTER_UPDATE_FAIL))
                            }
                        )
                }
            }
            is SecurityIntent.CheckCanChangePassword -> {
                previousState.securityInfo?.isWalletBackedUp?.let { isBackedUp ->
                    process(
                        SecurityIntent.UpdateViewState(
                            if (isBackedUp) {
                                SecurityViewState.LaunchPasswordChange
                            } else {
                                SecurityViewState.ShowMustBackWalletUp
                            }
                        )
                    )
                    null
                }
            }
            is SecurityIntent.ToggleCloudBackup -> {
                previousState.securityInfo?.isCloudBackupEnabled?.let { isCloudBackupEnabled ->
                    interactor.updateCloudBackup(!isCloudBackupEnabled)
                    process(SecurityIntent.UpdateCloudBackup(!isCloudBackupEnabled))
                    null
                }
            }
            is SecurityIntent.ClearPrefs -> {
                interactor.pinCodeValidatedForChange()
                null
            }
            is SecurityIntent.UpdateViewState,
            is SecurityIntent.UpdateSecurityInfo,
            is SecurityIntent.UpdateErrorState,
            is SecurityIntent.ResetErrorState,
            is SecurityIntent.BiometricsDisabled,
            is SecurityIntent.EnableBiometrics,
            is SecurityIntent.TwoFactorEnabled,
            is SecurityIntent.TwoFactorDisabled,
            is SecurityIntent.ResetViewState,
            is SecurityIntent.UpdateTorFiltering,
            is SecurityIntent.UpdateScreenshotsEnabled,
            is SecurityIntent.UpdateCloudBackup,
            -> null
        }.exhaustive
}
