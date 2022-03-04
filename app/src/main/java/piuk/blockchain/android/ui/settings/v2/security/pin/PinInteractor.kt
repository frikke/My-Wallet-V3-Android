package piuk.blockchain.android.ui.settings.v2.security.pin

import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.tasks.Task
import info.blockchain.wallet.api.data.UpdateType
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.auth.MobileNoticeDialog
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.then

class PinInteractor internal constructor(
    private val apiStatus: ApiStatus,
    private val persistentPrefs: PersistentPrefs,
    private val authDataManager: AuthDataManager,
    private val payloadManager: PayloadDataManager,
    private val pinRepository: PinRepository,
    private val biometricsController: BiometricsController,
    private val mobileNoticeRemoteConfig: MobileNoticeRemoteConfig,
    private val credentialsWiper: CredentialsWiper,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val authPrefs: AuthPrefs,
    private val defaultLabels: DefaultLabels,
    private val walletStatus: WalletStatus
) {

    fun shouldShowFingerprintLogin(): Boolean {
        return biometricsController.isBiometricUnlockEnabled && (!isCreatingNewPin() || !isConfirmingPin())
    }

    fun isCreatingNewPin(): Boolean = authPrefs.pinId.isEmpty()

    fun isConfirmingPin(): Boolean = isCreatingNewPin() && pinRepository.pin.isNotEmpty()

    fun updatePayload(password: String): Completable =
        payloadManager.initializeAndDecrypt(
            authPrefs.sharedKey,
            authPrefs.walletGuid,
            password
        ).then { verifyCloudBackup() }

    fun checkApiStatus(): Single<Boolean> = apiStatus.isHealthy()

    fun hasExceededPinAttempts(): Boolean = authPrefs.pinFails >= LOCAL_MAX_ATTEMPTS

    fun getTempPassword(): String? = payloadManager.tempPassword

    fun incrementFailureCount() {
        var fails = authPrefs.pinFails
        authPrefs.pinFails = ++fails
    }

    fun clearPin() {
        authPrefs.removePinID()
        authPrefs.pinId = ""
        pinRepository.clearPin()
    }

    fun resetPinFailureCount() {
        authPrefs.pinFails = 0
    }

    fun clearPrefs() {
        persistentPrefs.clear()
    }

    fun getCurrentPin(): String = pinRepository.pin

    fun fetchInfoMessage(): Single<MobileNoticeDialog> =
        mobileNoticeRemoteConfig.mobileNoticeDialog()

    fun createPin(tempPassword: String, pin: String): Completable =
        authDataManager.createPin(tempPassword, pin)
            .then { verifyCloudBackup() }

    fun checkForceUpgradeStatus(versionName: String): Observable<UpdateType> {
        return walletOptionsDataManager.checkForceUpgrade(versionName)
    }

    fun validatePIN(pin: String, isForValidatingPinForResult: Boolean = false): Single<String> =
        authDataManager.validatePin(pin)
            .firstOrError()
            .flatMap { validatedPin ->
                if (isForValidatingPinForResult) {
                    authDataManager.verifyCloudBackup().toSingle { validatedPin }
                } else {
                    Single.just(validatedPin)
                }
            }

    private fun verifyCloudBackup(): Completable =
        authDataManager.verifyCloudBackup()

    fun resetApp() {
        credentialsWiper.wipe()
    }

    fun validatePassword(password: String): Completable {
        return payloadManager.initializeAndDecrypt(
            authPrefs.sharedKey,
            authPrefs.walletGuid,
            password
        )
    }

    fun setAccountLabelIfNecessary() {
        if (walletStatus.isNewlyCreated &&
            payloadManager.accounts.isNotEmpty() &&
            payloadManager.getAccount(0).label.isEmpty()
        ) {
            payloadManager.getAccount(0).label =
                defaultLabels.getDefaultNonCustodialWalletLabel()
        }
    }

    fun updateShareKeyInPrefs() {
        authPrefs.sharedKey = payloadManager.wallet?.sharedKey.orEmpty()
    }

    fun isWalletUpgradeRequired(): Boolean = payloadManager.isWalletUpgradeRequired

    fun updateInfo(appUpdateManager: AppUpdateManager): Observable<Task<AppUpdateInfo>> {
        return Observable.fromCallable { appUpdateManager.appUpdateInfo }
    }

    fun doUpgradeWallet(secondPassword: String?, isFromPinCreation: Boolean): Completable {
        return payloadManager.upgradeWalletPayload(
            secondPassword,
            defaultLabels.getDefaultNonCustodialWalletLabel()
        )
    }

    companion object {
        private const val LOCAL_MAX_ATTEMPTS: Long = 4
    }
}
