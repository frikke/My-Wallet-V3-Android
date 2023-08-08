package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.analytics.events.WalletUpgradeEvent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.utils.thenSingle
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.net.SocketTimeoutException
import org.spongycastle.crypto.InvalidCipherTextException
import timber.log.Timber

class PinModel(
    initialState: PinState,
    mainScheduler: Scheduler,
    private val interactor: PinInteractor,
    private val specificAnalytics: ProviderSpecificAnalytics,
    environmentConfig: EnvironmentConfig,
    private val remoteLogger: RemoteLogger,
    private val analytics: Analytics,
) : MviModel<PinState, PinIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {

    override fun performAction(
        previousState: PinState,
        intent: PinIntent
    ): Disposable? =
        when (intent) {
            is PinIntent.CheckIntercomStatus -> interactor.getIntercomStatus()
                .subscribeBy(
                    onSuccess = { enabled ->
                        if (enabled) {
                            interactor.initialiseIntercom()
                        }
                        process(PinIntent.UpdateIntercomStatus(enabled))
                    },
                    onError = {
                        Timber.e("Error getting intercom status")
                    }
                )

            is PinIntent.GetAction -> {
                when {
                    interactor.isCreatingNewPin() -> process(PinIntent.UpdateAction(PinScreenView.CreateNewPin))
                    interactor.isConfirmingPin() -> process(PinIntent.UpdateAction(PinScreenView.ConfirmNewPin))
                    else -> process(PinIntent.UpdateAction(PinScreenView.LoginWithPin))
                }
                null
            }

            is PinIntent.CreatePIN -> {
                interactor.getTempPassword()?.let { tempPassword ->
                    interactor.createPin(tempPassword, intent.pin).thenSingle {
                        interactor.updatePayload(tempPassword).toSingle {
                            listOf(
                                handlePayloadUpdateComplete(true),
                                PinIntent.SetCanShowFingerprint(true)
                            )
                        }.onErrorResumeNext {
                            Single.just(listOf(handlePayloadUpdateError(it)))
                        }
                    }
                        .handleProgress()
                        .subscribeBy(
                            onSuccess = { intents ->
                                intents.forEach {
                                    process(it)
                                }
                                interactor.resetPinFailureCount()
                            },
                            onError = {
                                process(PinIntent.UpdatePinErrorState(PinError.CREATE_PIN_FAILED))
                                interactor.clearPrefs()
                                interactor.clearPin()
                            }
                        )
                } ?: run {
                    // TODO This shouldn't happen, we may have multiple instances of PayloadData
                    process(PinIntent.UpdatePinErrorState(PinError.CREATE_PIN_FAILED))
                    interactor.clearPrefs()
                    interactor.clearPin()
                    null
                }
            }

            is PinIntent.ValidatePIN -> {
                interactor.validatePIN(intent.pin, intent.isForValidatingPinForResult, previousState.isIntercomEnabled)
                    .flatMap { passw ->
                        if (intent.isForValidatingPinForResult || intent.isChangingPin) {
                            Single.just(listOf(PinIntent.ValidatePINSucceeded))
                        } else {
                            interactor.updatePayload(passw).toSingle {
                                listOf(
                                    PinIntent.SetCanShowFingerprint(true),
                                    handlePayloadUpdateComplete(false)
                                )
                            }.onErrorResumeNext {
                                Single.just(listOf(handlePayloadUpdateError(it)))
                            }
                        }
                    }
                    .handleProgress()
                    .subscribeBy(
                        onSuccess = { intents ->
                            intents.forEach {
                                process(it)
                            }
                            interactor.resetPinFailureCount()
                        },
                        onError = { throwable ->
                            Timber.e(throwable)
                            if (throwable is InvalidCredentialsException) {
                                interactor.incrementFailureCount()
                                process(PinIntent.ValidatePINFailed(PinError.INVALID_CREDENTIALS))
                                process(PinIntent.CheckNumPinAttempts)
                            } else {
                                process(PinIntent.UpdatePinErrorState(PinError.ERROR_CONNECTION))
                            }
                        }
                    )
            }

            is PinIntent.GetCurrentPin -> {
                process(PinIntent.SetCurrentPin(interactor.getCurrentPin()))
                null
            }

            is PinIntent.CheckNumPinAttempts -> {
                if (interactor.hasExceededPinAttempts()) {
                    process(PinIntent.UpdatePinErrorState(PinError.NUM_ATTEMPTS_EXCEEDED))
                }
                null
            }

            is PinIntent.ValidatePassword -> {
                interactor.validatePassword(intent.password)
                    .handleProgress()
                    .subscribeBy(
                        onComplete = {
                            process(PinIntent.UpdatePasswordErrorState(true))
                            interactor.clearPin()
                            remoteLogger.logEvent("new password. pin reset")
                        },
                        onError = { throwable -> handlePasswordValidatedError(throwable) }
                    )
            }

            is PinIntent.CheckApiStatus ->
                interactor.checkApiStatus()
                    .subscribeBy(
                        onSuccess = { isHealthy -> process(PinIntent.UpdateApiStatus(isHealthy)) },
                        onError = { Timber.e(it) }
                    )

            is PinIntent.CheckFingerprint -> {
                process(PinIntent.SetShowFingerprint(interactor.shouldShowFingerprintLogin()))
                null
            }

            is PinIntent.FetchRemoteMobileNotice -> {
                interactor.fetchInfoMessage()
                    .subscribeBy(
                        onSuccess = { mobileNoticeDialog ->
                            process(PinIntent.ShowMobileNoticeDialog(mobileNoticeDialog))
                        },
                        onError = {
                            if (it is NoSuchElementException) {
                                Timber.d("No mobile notice found")
                            } else {
                                Timber.e(it)
                            }
                        }
                    )
            }

            is PinIntent.UpgradeWallet -> {
                interactor.doUpgradeWallet(intent.secondPassword)
                    .handleProgress()
                    .subscribeBy(
                        onComplete = {
                            process(PinIntent.UpgradeWalletResponse(true))
                            analytics.logEvent(WalletUpgradeEvent(true))
                        },
                        onError = { throwable ->
                            remoteLogger.logException(throwable)
                            process(PinIntent.UpgradeWalletResponse(false))
                            analytics.logEvent(WalletUpgradeEvent(false))
                        }
                    )
            }

            is PinIntent.PinLogout -> {
                interactor.resetApp()
                null
            }

            is PinIntent.DisableBiometrics -> {
                interactor.disableBiometrics()
                null
            }

            is PinIntent.UpdateApiStatus,
            is PinIntent.ClearStateAlreadyHandled,
            is PinIntent.UpdatePinErrorState,
            is PinIntent.ValidatePINFailed,
            is PinIntent.ValidatePINSucceeded,
            is PinIntent.ShowMobileNoticeDialog,
            is PinIntent.UpdateAction,
            is PinIntent.SetCurrentPin,
            is PinIntent.SetCanShowFingerprint,
            is PinIntent.AppNeedsUpgrade,
            is PinIntent.PayloadSucceeded,
            is PinIntent.UpdatePasswordErrorState,
            is PinIntent.UpdatePayloadErrorState,
            is PinIntent.UpgradeRequired,
            is PinIntent.UpgradeWalletResponse,
            is PinIntent.SetShowFingerprint,
            PinIntent.DialogShown,
            PinIntent.CreatePINSucceeded,
            is PinIntent.UpdateLoading,
            is PinIntent.UpdateIntercomStatus -> null
        }

    private fun handlePasswordValidatedError(throwable: Throwable) {
        remoteLogger.logException(throwable, "Pin Model")
        when (throwable) {
            is ServerConnectionException ->
                process(PinIntent.UpdatePasswordErrorState(errorState = PasswordError.SERVER_CONNECTION_EXCEPTION))

            is SocketTimeoutException ->
                process(PinIntent.UpdatePasswordErrorState(errorState = PasswordError.SERVER_TIMEOUT))

            is HDWalletException -> {
                process(PinIntent.UpdatePasswordErrorState(errorState = PasswordError.HD_WALLET_EXCEPTION))
            }

            is AccountLockedException ->
                process(PinIntent.UpdatePasswordErrorState(errorState = PasswordError.ACCOUNT_LOCKED))

            else -> {
                process(PinIntent.UpdatePasswordErrorState(errorState = PasswordError.UNKNOWN))
            }
        }
    }

    private fun handlePayloadUpdateComplete(isFromPinCreation: Boolean = false): PinIntent {
        interactor.updateShareKeyInPrefs()
        specificAnalytics.logLogin(true)

        return if (interactor.isWalletUpgradeRequired()) {
            PinIntent.UpgradeRequired(true, isFromPinCreation, SECOND_PASSWORD_ATTEMPTS)
        } else {
            PinIntent.PayloadSucceeded(isFromPinCreation)
        }
    }

    private fun handlePayloadUpdateError(throwable: Throwable): PinIntent {
        specificAnalytics.logLogin(false)
        remoteLogger.logException(throwable, "Pin Model")
        return when (throwable) {
            is InvalidCredentialsException ->
                PinIntent.UpdatePayloadErrorState(PayloadError.CREDENTIALS_INVALID)

            is ServerConnectionException ->
                PinIntent.UpdatePayloadErrorState(PayloadError.SERVER_CONNECTION_EXCEPTION)

            is SocketTimeoutException ->
                PinIntent.UpdatePayloadErrorState(PayloadError.SERVER_TIMEOUT)

            is UnsupportedVersionException ->
                PinIntent.UpdatePayloadErrorState(PayloadError.UNSUPPORTED_VERSION_EXCEPTION)

            is DecryptionException ->
                PinIntent.UpdatePayloadErrorState(PayloadError.DECRYPTION_EXCEPTION)

            is HDWalletException -> {
                PinIntent.UpdatePayloadErrorState(PayloadError.HD_WALLET_EXCEPTION)
            }

            is InvalidCipherTextException -> {
                PinIntent.UpdatePayloadErrorState(PayloadError.INVALID_CIPHER_TEXT).also {
                    interactor.clearPin()
                }
            }

            is AccountLockedException -> PinIntent.UpdatePayloadErrorState(PayloadError.ACCOUNT_LOCKED)
            else -> {
                PinIntent.UpdatePayloadErrorState(PayloadError.UNKNOWN)
            }
        }
    }

    private fun Completable.handleProgress() =
        this.doOnSubscribe { process(PinIntent.UpdateLoading(true)) }
            .doFinally { process(PinIntent.UpdateLoading(false)) }

    private fun <T : Any> Single<T>.handleProgress() =
        this.doOnSubscribe { process(PinIntent.UpdateLoading(true)) }
            .doFinally { process(PinIntent.UpdateLoading(false)) }

    companion object {
        const val SECOND_PASSWORD_ATTEMPTS = 5
    }
}
