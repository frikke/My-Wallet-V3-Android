package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.metadata.MetadataInitException
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber

class LoaderModel(
    initialState: LoaderState,
    environmentConfig: EnvironmentConfig,
    mainScheduler: Scheduler,
    private val crashLogger: CrashLogger,
    private val appUtil: AppUtil,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val prerequisites: Prerequisites,
    private val authPrefs: AuthPrefs,
    private val interactor: LoaderInteractor
) : MviModel<LoaderState, LoaderIntents>(initialState, mainScheduler, environmentConfig, crashLogger) {
    override fun performAction(previousState: LoaderState, intent: LoaderIntents): Disposable? {
        return when (intent) {
            is LoaderIntents.CheckIsLoggedIn -> checkIsLoggedIn(intent.isPinValidated, intent.isAfterWalletCreation)
            is LoaderIntents.OnTermsAndConditionsSigned -> {
                process(LoaderIntents.StartMainActivity(null, false))
                null
            }
            is LoaderIntents.OnEmailVerificationFinished -> {
                process(LoaderIntents.StartMainActivity(null, true))
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

    private fun checkIsLoggedIn(isPinValidated: Boolean, isAfterWalletCreation: Boolean): Disposable? {

        val hasLoginInfo = authPrefs.walletGuid.isNotEmpty() && prefs.pinId.isNotEmpty()

        return when {
            // App has been PIN validated
            hasLoginInfo && isPinValidated -> {
                interactor.loaderIntents.subscribe {
                    process(it)
                }
                interactor.initSettings(isAfterWalletCreation)
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
            process(LoaderIntents.HideMetadataNodeFailure)
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

    private fun showToast(toastType: ToastType) {
        process(LoaderIntents.ShowToast(toastType))
        process(LoaderIntents.ResetToast)
    }

    private fun logException(throwable: Throwable) {
        crashLogger.logEvent("Startup exception: ${throwable.message}")
        crashLogger.logException(throwable)
    }
}
