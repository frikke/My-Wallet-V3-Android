package piuk.blockchain.android.ui.auth

import android.annotation.SuppressLint
import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.ProviderSpecificAnalytics
import com.blockchain.notifications.analytics.WalletUpgradeEvent
import com.blockchain.wallet.DefaultLabels
import info.blockchain.wallet.api.data.UpdateType
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.SocketTimeoutException
import org.spongycastle.crypto.InvalidCipherTextException
import piuk.blockchain.android.R
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

class PinEntryPresenter(
    private val analytics: Analytics,
    private val specificAnalytics: ProviderSpecificAnalytics,
    private val authDataManager: AuthDataManager,
    private val appUtil: AppUtil,
    private val prefs: PersistentPrefs,
    private val payloadDataManager: PayloadDataManager,
    private val defaultLabels: DefaultLabels,
    private val pinRepository: PinRepository,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val mobileNoticeRemoteConfig: MobileNoticeRemoteConfig,
    private val crashLogger: CrashLogger,
    private val apiStatus: ApiStatus,
    private val credentialsWiper: CredentialsWiper,
    private val biometricsController: BiometricsController
) : BasePresenter<PinEntryView>() {

    @VisibleForTesting
    var canShowFingerprintDialog = true

    @VisibleForTesting
    var isForValidatingPinForResult = false

    @VisibleForTesting
    var isForValidatingAndLoadingPayloadResult = false

    @VisibleForTesting
    var userEnteredPin = ""

    @VisibleForTesting
    var userEnteredConfirmationPin: String? = null

    @VisibleForTesting
    internal var bAllowExit = true

    internal val ifShouldShowFingerprintLogin: Boolean
        get() = (
            !(isForValidatingPinForResult || isCreatingNewPin) &&
                biometricsController.isBiometricUnlockEnabled
            )

    val isCreatingNewPin: Boolean
        get() = prefs.pinId.isEmpty()

    private val isChangingPin: Boolean
        get() = isCreatingNewPin && pinRepository.pin.isNotEmpty()

    override fun onViewReady() {
        isForValidatingPinForResult = view.isForValidatingPinForResult
        isForValidatingAndLoadingPayloadResult = view.isForValidatingAndLoadingPayloadResult

        checkPinFails()
        checkFingerprintStatus()
        setupCommitHash()
        checkApiStatus()
    }

    private fun checkApiStatus() {
        compositeDisposable += apiStatus.isHealthy()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { isHealthy ->
                if (isHealthy.not())
                    view?.showApiOutageMessage()
            }, onError = {
                Timber.e(it)
            })
    }

    private fun setupCommitHash() {
        view.setupCommitHashView()
    }

    fun checkFingerprintStatus() {
        if (ifShouldShowFingerprintLogin) {
            view.showFingerprintDialog()
        } else {
            view.showKeyboard()
        }
    }

    fun canShowFingerprintDialog(): Boolean {
        return canShowFingerprintDialog
    }

    fun loginWithDecryptedPin(pincode: String) {
        canShowFingerprintDialog = false
        view.fillPinBoxes()
        validatePIN(pincode)
    }

    fun onDeleteClicked() {
        if (userEnteredPin.isNotEmpty()) {
            // Remove last char from pin string
            userEnteredPin = userEnteredPin.substring(0, userEnteredPin.length - 1)

            // Clear last box
            view.clearPinBoxAtIndex(userEnteredPin.length)
        }
    }

    fun onPadClicked(string: String?) {
        if (string == null || userEnteredPin.length == PIN_LENGTH) {
            return
        }

        // Append tapped #
        userEnteredPin += string

        for (i in userEnteredPin.indices) {
            // Ensures that all necessary dots are filled
            view.fillPinBoxAtIndex(i)
        }

        // Perform appropriate action if PIN_LENGTH has been reached
        if (userEnteredPin.length == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (userEnteredPin == "0000") {
                showErrorSnackbar(R.string.zero_pin) {
                }
                clearPinViewAndReset()
                if (isCreatingNewPin) {
                    view.setTitleString(R.string.create_pin)
                }
                return
            }

            if (userEnteredConfirmationPin == null) {
                analytics.logEventOnce(AnalyticsEvents.WalletSignupPINFirst)
            } else {
                analytics.logEventOnce(AnalyticsEvents.WalletSignupPINSecond)
            }

            // Only show warning on first entry and if user is creating a new PIN
            if (isCreatingNewPin && isPinCommon(userEnteredPin) && userEnteredConfirmationPin == null) {
                view.showCommonPinWarning(
                    object : DialogButtonCallback {
                        override fun onPositiveClicked() {
                            clearPinViewAndReset()
                        }

                        override fun onNegativeClicked() {
                            validateAndConfirmPin()
                        }
                    }
                )
                // If user is changing their PIN and it matches their old one, disallow it
            } else if (isChangingPin && userEnteredConfirmationPin == null &&
                pinRepository.pin == userEnteredPin
            ) {
                showErrorSnackbar(R.string.change_pin_new_matches_current)
                clearPinViewAndReset()
            } else {
                validateAndConfirmPin()
            }
        }
    }

    internal fun validateAndConfirmPin() {
        // Validate
        when {
            prefs.pinId.isNotEmpty() -> {
                view.setTitleVisibility(View.INVISIBLE)
                validatePIN(userEnteredPin)
            }
            userEnteredConfirmationPin == null -> {
                // End of Create -  Change to Confirm
                userEnteredConfirmationPin = userEnteredPin
                userEnteredPin = ""
                view.setTitleString(R.string.confirm_pin)
                clearPinBoxes()
            }
            userEnteredConfirmationPin == userEnteredPin -> {
                // End of Confirm - Pin is confirmed
                createNewPin(userEnteredPin)
            }
            else -> {
                // End of Confirm - Pin Mismatch
                showErrorSnackbar(R.string.pin_mismatch_error) {
                }
                view.setTitleString(R.string.create_pin)
                clearPinViewAndReset()
            }
        }
    }

    /**
     * Resets the view without restarting the page
     */
    internal fun clearPinViewAndReset() {
        clearPinBoxes()
        userEnteredConfirmationPin = null
        checkFingerprintStatus()
    }

    fun clearPinBoxes() {
        userEnteredPin = ""
        view?.clearPinBoxes()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun updatePayload(password: String, isFromPinCreation: Boolean = false) {
        compositeDisposable += payloadDataManager.initializeAndDecrypt(
            prefs.sharedKey,
            prefs.walletGuid,
            password
        )
            .then { verifyCloudBackup() }
            .handleProgress(R.string.decrypting_wallet)
            .subscribeBy(
                onComplete = {
                    canShowFingerprintDialog = true
                    handlePayloadUpdateComplete(isFromPinCreation)
                },
                onError = { handlePayloadUpdateError(it) }
            )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun handlePayloadUpdateComplete(isFromPinCreation: Boolean = false) {
        val wallet = payloadDataManager.wallet!!
        prefs.sharedKey = wallet.sharedKey

        setAccountLabelIfNecessary()

        specificAnalytics.logLogin(true)

        if (payloadDataManager.isWalletUpgradeRequired) {
            view?.walletUpgradeRequired(SECOND_PASSWORD_ATTEMPTS, isFromPinCreation)
        } else {
            onUpdateFinished(isFromPinCreation)
        }
    }

    fun doUpgradeWallet(secondPassword: String?, isFromPinCreation: Boolean) {
        // v2 -> v3 -> v4
        compositeDisposable += payloadDataManager.upgradeWalletPayload(
            secondPassword,
            defaultLabels.getDefaultNonCustodialWalletLabel()
        )
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .handleProgress(R.string.upgrading)
            .subscribeBy(
                onComplete = {
                    view.dismissProgressDialog()
                    onUpdateFinished(isFromPinCreation)
                    analytics.logEvent(WalletUpgradeEvent(true))
                },
                onError = { throwable ->
                    analytics.logEvent(WalletUpgradeEvent(false))
                    crashLogger.logException(throwable)
                    view.onWalletUpgradeFailed()
                }
            )
    }

    private fun onUpdateFinished(isFromPinCreation: Boolean) {
        if (isFromPinCreation && biometricsController.isBiometricAuthEnabled) {
            view.askToUseBiometrics()
        } else {
            view.restartAppWithVerifiedPin()
        }
    }

    private fun verifyCloudBackup(): Completable = authDataManager.verifyCloudBackup()

    fun finishSignupProcess() {
        view.restartAppWithVerifiedPin()
    }

    private fun handlePayloadUpdateError(t: Throwable) {
        specificAnalytics.logLogin(false)
        when (t) {
            is InvalidCredentialsException -> view.goToPasswordRequiredActivity()
            is ServerConnectionException,
            is SocketTimeoutException -> {
                showFatalErrorSnackbarAndRestart(R.string.server_unreachable_exit, t)
            }
            is UnsupportedVersionException -> view.showWalletVersionNotSupportedDialog(t.message)
            is DecryptionException -> view.goToPasswordRequiredActivity()
            is HDWalletException -> {
                // This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                showFatalErrorSnackbarAndRestart(R.string.unexpected_error, t)
            }
            is InvalidCipherTextException -> {
                // Password changed on web, needs re-pairing
                crashLogger.logEvent("password changed elsewhere. Pin is reset")
                pinRepository.clearPin()
                appUtil.clearCredentials()
                showFatalErrorSnackbarAndRestart(R.string.password_changed_explanation, t)
            }
            is AccountLockedException -> view.showAccountLockedDialog()
            else -> {
                showFatalErrorSnackbarAndRestart(R.string.unexpected_error, t)
            }
        }
    }

    fun validatePassword(password: String) {
        compositeDisposable += payloadDataManager.initializeAndDecrypt(
            prefs.sharedKey,
            prefs.walletGuid,
            password
        )
            .handleProgress(R.string.validating_password)
            .subscribeBy(
                onComplete = { handlePasswordValidated() },
                onError = { throwable -> handlePasswordValidatedError(throwable) }
            )
    }

    private fun handlePasswordValidated() {
        showMessageSnackbar(R.string.pin_4_strikes_password_accepted) {
            view?.restartPageAndClearTop()
        }
        prefs.removeValue(PersistentPrefs.KEY_PIN_FAILS)
        prefs.pinId = ""
        crashLogger.logEvent("new password. pin reset")
        pinRepository.clearPin()
    }

    private fun handlePasswordValidatedError(t: Throwable) {
        when (t) {
            is ServerConnectionException,
            is SocketTimeoutException ->
                showFatalErrorSnackbarAndRestart(R.string.server_unreachable_exit, t)
            is HDWalletException -> {
                // This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                showFatalErrorSnackbarAndRestart(R.string.unexpected_error, t)
            }
            is AccountLockedException -> view.showAccountLockedDialog()
            else -> {
                crashLogger.logException(t)
                showErrorSnackbar(R.string.invalid_password)
                view.showValidationDialog()
            }
        }
    }

    private fun createNewPin(pin: String) {
        val tempPassword = payloadDataManager.tempPassword
        if (tempPassword == null) {
            showErrorSnackbar(R.string.create_pin_failed) {
                appUtil.restartApp()
            }
            prefs.clear()
            return
        }

        compositeDisposable += authDataManager.createPin(tempPassword, pin)
            .then { verifyCloudBackup() }
            .handleProgress(R.string.creating_pin)
            .subscribeBy(
                onComplete = {
                    biometricsController.setBiometricUnlockDisabled()
                    prefs.pinFails = 0
                    updatePayload(tempPassword, true)
                },
                onError = {
                    showErrorSnackbar(R.string.create_pin_failed) {
                        appUtil.restartApp()
                    }
                    prefs.clear()
                }
            )
    }

    @SuppressLint("CheckResult")
    private fun validatePIN(pin: String) {
        authDataManager.validatePin(pin)
            .firstOrError()
            .flatMap { validatedPin ->
                if (isForValidatingPinForResult) {
                    verifyCloudBackup().toSingle { validatedPin }
                } else {
                    Single.just(validatedPin)
                }
            }
            .handleProgress(R.string.validating_pin)
            .subscribeBy(
                onSuccess = { password ->
                    if (password != null) {
                        if (isForValidatingPinForResult) {
                            view.finishWithResultOk(pin)
                        } else {
                            updatePayload(password)
                        }
                        prefs.pinFails = 0
                    } else {
                        handleValidateFailure()
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    if (throwable is InvalidCredentialsException) {
                        handleValidateFailure()
                    } else {
                        showErrorSnackbar(R.string.api_fail) {
                            view?.restartPageAndClearTop()
                        }
                    }
                }
            )
    }

    private fun handleValidateFailure() {
        if (isForValidatingPinForResult) {
            incrementFailureCount()
        } else {
            incrementFailureCountAndRestart()
        }
    }

    private fun incrementFailureCount() {
        var fails = prefs.pinFails
        prefs.pinFails = ++fails
        showErrorSnackbar(R.string.invalid_pin)
        userEnteredPin = ""
        view.clearPinBoxes()
        view.setTitleVisibility(View.VISIBLE)
        view.setTitleString(R.string.pin_entry)
    }

    fun incrementFailureCountAndRestart() {
        var fails = prefs.pinFails
        prefs.pinFails = ++fails
        showErrorSnackbar(R.string.invalid_pin) {
            view?.restartPageAndClearTop()
        }
    }

    // Check user's password if PIN fails >= max
    private fun checkPinFails() {
        val fails = prefs.pinFails
        getPinRetriesFromRemoteConfig { maxAttempts ->
            if (fails >= maxAttempts) {
                showMaxAttemptsSnackbar(maxAttempts)
                view.showMaxAttemptsDialog()
            }
        }
    }

    private fun setAccountLabelIfNecessary() {
        if (prefs.isNewlyCreated &&
            payloadDataManager.accounts.isNotEmpty() &&
            payloadDataManager.getAccount(0).label.isEmpty()
        ) {
            payloadDataManager.getAccount(0).label =
                defaultLabels.getDefaultNonCustodialWalletLabel()
        }
    }

    private fun isPinCommon(pin: String): Boolean {
        val commonPins = listOf("1234", "1111", "1212", "7777", "1004")
        return commonPins.contains(pin)
    }

    fun resetApp() {
        credentialsWiper.wipe()
    }

    fun allowExit(): Boolean {
        return bAllowExit
    }

    @Suppress("SameParameterValue")
    @UiThread
    private fun showMessageSnackbar(@StringRes message: Int, doOnDismiss: () -> Unit = {}) {
        view?.showSnackbar(message, SnackbarType.Success, doOnDismiss)
    }

    @UiThread
    private fun showErrorSnackbar(@StringRes message: Int, doOnDismiss: () -> Unit = {}) {
        view?.dismissProgressDialog()
        view?.showSnackbar(message, SnackbarType.Error, doOnDismiss)
    }

    @UiThread
    private fun showMaxAttemptsSnackbar(maxAttempts: Int) {
        view?.dismissProgressDialog()
        view?.showParameteredSnackbar(R.string.pin_max_strikes, SnackbarType.Error, maxAttempts) {}
    }

    private class PinEntryLogException(cause: Throwable) : Exception(cause)

    @UiThread
    private fun showFatalErrorSnackbarAndRestart(@StringRes message: Int, t: Throwable) {
        view?.showSnackbar(message, SnackbarType.Error) {
            appUtil.restartApp()
        }
        crashLogger.logException(PinEntryLogException(t))
    }

    internal fun clearLoginState() {
        appUtil.logout()
    }

    fun fetchInfoMessage() {
        compositeDisposable += mobileNoticeRemoteConfig.mobileNoticeDialog()
            .subscribeBy(
                onSuccess = { view.showMobileNotice(it) },
                onError = {
                    if (it is NoSuchElementException)
                        Timber.d("No mobile notice found")
                    else
                        Timber.e(it)
                }
            )
    }

    fun checkForceUpgradeStatus(versionName: String) {
        compositeDisposable += walletOptionsDataManager.checkForceUpgrade(versionName)
            .subscribeBy(
                onNext = { updateType ->
                    if (updateType !== UpdateType.NONE)
                        view.appNeedsUpgrade(updateType === UpdateType.FORCE)
                },
                onError = { Timber.e(it) }
            )
    }

    private fun getPinRetriesFromRemoteConfig(action: (Int) -> Unit) {
        compositeDisposable += Single.just(LOCAL_MAX_ATTEMPTS)
            .onErrorReturn { LOCAL_MAX_ATTEMPTS }
            .subscribeBy(
                onSuccess = {
                    action.invoke(it.toInt())
                }, onError = {
                Timber.d("Error getting PIN tries from remote config: $it")
            }
            )
    }

    companion object {
        private const val PIN_LENGTH = 4
        private const val LOCAL_MAX_ATTEMPTS: Long = 4
        private const val SECOND_PASSWORD_ATTEMPTS = 5
    }

    private fun Completable.handleProgress(@StringRes msg: Int) =
        this.doOnSubscribe { view.showProgressDialog(msg) }
            .doFinally { view.dismissProgressDialog() }

    private fun <T> Single<T>.handleProgress(@StringRes msg: Int) =
        this.doOnSubscribe { view.showProgressDialog(msg) }
            .doFinally { view.dismissProgressDialog() }
}
