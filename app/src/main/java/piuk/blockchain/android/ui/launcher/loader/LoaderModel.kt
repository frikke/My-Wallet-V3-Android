package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.metadata.MetadataInitException
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.SuperAppMvpPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.util.AppUtil
import timber.log.Timber

class LoaderModel(
    initialState: LoaderState,
    environmentConfig: EnvironmentConfig,
    mainScheduler: Scheduler,
    private val remoteLogger: RemoteLogger,
    private val appUtil: AppUtil,
    private val payloadDataManager: PayloadDataManager,
    private val prerequisites: Prerequisites,
    private val authPrefs: AuthPrefs,
    private val interactor: LoaderInteractor,
    private val walletModeService: WalletModeService,
    private val educationalScreensPrefs: SuperAppMvpPrefs
) : MviModel<LoaderState, LoaderIntents>(initialState, mainScheduler, environmentConfig, remoteLogger) {
    override fun performAction(previousState: LoaderState, intent: LoaderIntents): Disposable? {
        return when (intent) {
            is LoaderIntents.CheckIsLoggedIn -> checkIsLoggedIn(
                intent.isPinValidated,
                intent.loginMethod,
                intent.referralCode
            )

            is LoaderIntents.LaunchDashboard -> {
                launchDashboard(
                    loginMethod = previousState.loginMethod,
                    data = intent.data,
                    shouldLaunchUiTour = intent.shouldLaunchUiTour,
                    isUserInCowboysPromo = previousState.isUserInCowboysPromo
                )
                null
            }
            is LoaderIntents.UpdateLoadingStep -> {
                (intent.loadingStep as? LoadingStep.Error)?.let { error ->
                    handleError(error.throwable)
                }
                null
            }
            is LoaderIntents.DecryptAndSetupMetadata -> decryptAndSetupMetadata(intent.secondPassword)
            else -> null
        }
    }

    private fun checkIsLoggedIn(
        isPinValidated: Boolean,
        loginMethod: LoginMethod,
        referralCode: String?
    ): Disposable? {

        val hasLoginInfo = authPrefs.walletGuid.isNotEmpty() && authPrefs.pinId.isNotEmpty()

        return when {
            // App has been PIN validated
            hasLoginInfo && isPinValidated -> {
                interactor.loaderIntents.subscribe {
                    process(it)
                }
                interactor.initSettings(
                    isAfterWalletCreation = loginMethod == LoginMethod.WALLET_CREATION,
                    referralCode = referralCode
                )
            }
            else -> {
                process(LoaderIntents.StartLauncherActivity)
                null
            }
        }
    }

    private fun handleError(throwable: Throwable) {
        logException(throwable)
        process(LoaderIntents.UpdateProgressStep(ProgressStep.FINISH))
        if (throwable is InvalidCredentialsException || throwable is HDWalletException) {
            if (payloadDataManager.isDoubleEncrypted) {
                // Wallet double encrypted and needs to be decrypted to set up ether wallet, contacts etc
                process(LoaderIntents.ShowSecondPasswordDialog)
                process(LoaderIntents.HideSecondPasswordDialog)
            } else {
                showToast(ToastType.UNEXPECTED_ERROR)
                process(LoaderIntents.UpdateLoadingStep(LoadingStep.RequestPin))
            }
        } else if (throwable is MetadataInitException) {
            process(LoaderIntents.ShowMetadataNodeFailure)
        } else {
            showToast(ToastType.UNEXPECTED_ERROR)
            process(LoaderIntents.UpdateLoadingStep(LoadingStep.RequestPin))
        }
    }

    private fun decryptAndSetupMetadata(secondPassword: String): Disposable? {
        return if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            showToast(ToastType.INVALID_PASSWORD)
            process(LoaderIntents.ShowSecondPasswordDialog)
            process(LoaderIntents.HideSecondPasswordDialog)
            null
        } else {
            prerequisites.decryptAndSetupMetadata(secondPassword)
                .doOnSubscribe {
                    process(LoaderIntents.UpdateProgressStep(ProgressStep.DECRYPTING_WALLET))
                }.subscribeBy(
                    onError = {
                        process(LoaderIntents.UpdateProgressStep(ProgressStep.FINISH))
                        Timber.e(it)
                    },
                    onComplete = {
                        process(LoaderIntents.UpdateProgressStep(ProgressStep.FINISH))
                        appUtil.loadAppWithVerifiedPin(
                            LoaderActivity::class.java
                        )
                    }
                )
        }
    }

    private fun launchDashboard(
        loginMethod: LoginMethod,
        data: String?,
        shouldLaunchUiTour: Boolean,
        isUserInCowboysPromo: Boolean
    ) {
        process(
            when {

                // Wallet mode switch enabled
                // + have not seen educational screen yet
                // + did not come from signup (already logged in)
                // -> show educational screen
                walletModeService.enabledWalletMode() != WalletMode.UNIVERSAL &&
                    educationalScreensPrefs.hasSeenEducationalWalletMode.not() &&
                    loginMethod == LoginMethod.PIN -> {
                    LoaderIntents.StartEducationalWalletModeActivity(
                        data = data
                    )
                }
                isUserInCowboysPromo -> {
                    LoaderIntents.StartMainActivity(data, false)
                }
                else -> {
                    LoaderIntents.StartMainActivity(data, shouldLaunchUiTour)
                }
            }
        )
    }

    private fun showToast(toastType: ToastType) {
        process(LoaderIntents.ShowToast(toastType))
        process(LoaderIntents.ResetToast)
    }

    private fun logException(throwable: Throwable) {
        remoteLogger.logEvent("Startup exception: ${throwable.message}")
        remoteLogger.logException(throwable)
    }
}
