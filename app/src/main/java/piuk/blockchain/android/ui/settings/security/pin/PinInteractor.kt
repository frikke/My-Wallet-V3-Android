package piuk.blockchain.android.ui.settings.security.pin

import android.app.Application
import androidx.annotation.VisibleForTesting
import com.blockchain.core.access.PinRepository
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.walletoptions.WalletOptionsDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.SessionPrefs
import com.blockchain.utils.then
import com.blockchain.wallet.DefaultLabels
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.tasks.Task
import info.blockchain.wallet.api.data.UpdateType
import io.intercom.android.sdk.Intercom
import io.intercom.android.sdk.IntercomError
import io.intercom.android.sdk.IntercomStatusCallback
import io.intercom.android.sdk.identity.Registration
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.auth.MobileNoticeDialog
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.home.CredentialsWiper

class PinInteractor internal constructor(
    private val apiStatus: ApiStatus,
    private val sessionPrefs: SessionPrefs,
    private val authDataManager: AuthDataManager,
    private val payloadManager: PayloadDataManager,
    private val pinRepository: PinRepository,
    private val biometricsController: BiometricsController,
    private val mobileNoticeRemoteConfig: MobileNoticeRemoteConfig,
    private val credentialsWiper: CredentialsWiper,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val authPrefs: AuthPrefs,
    private val defaultLabels: DefaultLabels,
    private val remoteLogger: RemoteLogger,
    private val isIntercomEnabledFlag: FeatureFlag,
    private val application: Application
) {

    fun shouldShowFingerprintLogin(): Boolean {
        return biometricsController.isBiometricUnlockEnabled && (!isCreatingNewPin() || !isConfirmingPin())
    }

    fun disableBiometrics() {
        biometricsController.setBiometricUnlockDisabled()
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
        sessionPrefs.clear()
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

    fun validatePIN(
        pin: String,
        isForValidatingPinForResult: Boolean = false,
        isIntercomEnabled: Boolean
    ): Single<String> =
        authDataManager.validatePin(pin)
            .firstOrError()
            .flatMap { validatedPin ->
                if (isIntercomEnabled) {
                    registerIntercomUser()
                }

                if (isForValidatingPinForResult) {
                    authDataManager.verifyCloudBackup().toSingle { validatedPin }
                } else {
                    Single.just(validatedPin)
                }
            }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun registerIntercomUser() {
        // TODO(dserrano): Move this initialization back to BlockchainApplication when the flag is removed
        Intercom.initialize(application, BuildConfig.INTERCOM_API_KEY, BuildConfig.INTERCOM_APP_ID)

        val registration = Registration.create().withUserId(authPrefs.walletGuid)
        Intercom.client().loginIdentifiedUser(
            registration,
            object : IntercomStatusCallback {
                override fun onFailure(intercomError: IntercomError) {
                    remoteLogger.logEvent("registerIntercomUser on PinInteractor " + intercomError.errorMessage)
                }

                override fun onSuccess() {
                    // Do nothing
                }
            }
        )
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

    fun updateShareKeyInPrefs() {
        authPrefs.sharedKey = payloadManager.wallet?.sharedKey.orEmpty()
    }

    fun isWalletUpgradeRequired(): Boolean = payloadManager.isWalletUpgradeRequired

    fun updateInfo(appUpdateManager: AppUpdateManager): Observable<Task<AppUpdateInfo>> {
        return Observable.fromCallable { appUpdateManager.appUpdateInfo }
    }

    fun doUpgradeWallet(secondPassword: String?): Completable {
        return payloadManager.upgradeWalletPayload(
            secondPassword,
            defaultLabels.getDefaultNonCustodialWalletLabel()
        )
    }

    fun getIntercomStatus(): Single<Boolean> = isIntercomEnabledFlag.enabled

    companion object {
        private const val LOCAL_MAX_ATTEMPTS: Long = 4
    }
}
