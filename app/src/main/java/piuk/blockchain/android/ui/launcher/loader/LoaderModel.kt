package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.metadata.MetadataInitException
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber

class LoaderModel(
    initialState: LoaderState,
    environmentConfig: EnvironmentConfig,
    private val mainScheduler: Scheduler,
    private val crashLogger: CrashLogger,
    private val appUtil: AppUtil,
    private val userIdentity: UserIdentity,
    private val analytics: Analytics,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val prerequisites: Prerequisites,
    private val authPrefs: AuthPrefs,
    private val interactor: LoaderInteractor
) : MviModel<LoaderState, LoaderIntents>(initialState, mainScheduler, environmentConfig, crashLogger) {
    override fun performAction(previousState: LoaderState, intent: LoaderIntents): Disposable? {
        return when (intent) {
            is LoaderIntents.CheckIsLoggedIn -> checkIsLoggedIn(intent.isPinValidated, intent.isAfterWalletCreation)
            is LoaderIntents.OnEmailVerificationFinished -> onEmailVerificationFinished()
            is LoaderIntents.DecryptAndSetupMetadata -> decryptAndSetupMetadata(intent.secondPassword)
            else -> null
        }
    }

    private fun checkIsLoggedIn(isPinValidated: Boolean, isAfterWalletCreation: Boolean): Disposable? {

        val hasLoginInfo = authPrefs.walletGuid.isNotEmpty() && prefs.pinId.isNotEmpty()

        return when {
            // App has been PIN validated
            hasLoginInfo && isPinValidated -> {
                interactor.initSettings(isAfterWalletCreation)
                    .doOnError {
                        handleError(it)
                    }
                    .subscribe {
                        process(it)
                    }
            }
            else -> {
                process(LoaderIntents.StartLauncherActivity)
                null
            }
        }
    }

    private fun onEmailVerificationFinished() =
        userIdentity.isEligibleFor(Feature.SimplifiedDueDiligence)
            .onErrorReturn { false }
            .doOnSuccess {
                if (it)
                    analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE)
            }
            .subscribeBy(
                onSuccess = {
                    process(LoaderIntents.StartMainActivity(null, it))
                }, onError = {}
            )

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
                process(LoaderIntents.UpdateLoaderStep(LoaderStep.RequestPin))
            }
        } else if (throwable is MetadataInitException) {
            process(LoaderIntents.ShowMetadataNodeFailure)
            process(LoaderIntents.HideMetadataNodeFailure)
        } else {
            showToast(ToastType.UNEXPECTED_ERROR)
            process(LoaderIntents.UpdateLoaderStep(LoaderStep.RequestPin))
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
