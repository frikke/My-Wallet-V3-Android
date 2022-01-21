package piuk.blockchain.android.ui.settings.v2.security

import com.blockchain.preferences.SecurityPrefs
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class SecurityInteractor internal constructor(
    private val settingsDataManager: SettingsDataManager,
    private val biometricsController: BiometricsController,
    private val securityPrefs: SecurityPrefs,
    private val pinRepository: PinRepository
) {

    fun loadInitialInformation(): Single<SecurityInfo> =
        Singles.zip(
            Single.just(biometricsController.isHardwareDetected),
            Single.just(biometricsController.isBiometricUnlockEnabled),
            settingsDataManager.getSettings().firstOrError()
        ).map { (biometricHardwareEnabled, biometricsEnabled, settings) ->
            SecurityInfo(
                isBiometricsVisible = biometricHardwareEnabled,
                isBiometricsEnabled = biometricsEnabled,
                isTorFilteringEnabled = settings.isBlockTorIps,
                areScreenshotsEnabled = securityPrefs.areScreenshotsEnabled,
                isTwoFaEnabled = settings.authType != Settings.AUTH_TYPE_OFF
            )
        }

    fun checkTwoFaState(): Single<SecurityIntent> =
        settingsDataManager.getSettings().firstOrError().flatMap {
            if (it.authType == Settings.AUTH_TYPE_OFF) {
                if (!it.isSmsVerified) {
                    Single.just(SecurityIntent.UpdateViewState(SecurityViewState.ShowVerifyPhoneNumberRequired))
                } else {
                    // TODO here we currently show a dialog with details about how to set up another type of two fa via website
                    settingsDataManager.updateTwoFactor(Settings.AUTH_TYPE_SMS).firstOrError().map {
                        SecurityIntent.TwoFactorEnabled
                    }
                }
            } else {
                if (it.authType == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR ||
                    it.authType == Settings.AUTH_TYPE_YUBI_KEY
                ) {
                    Single.just(SecurityIntent.UpdateViewState(SecurityViewState.ShowDisablingOnWebRequired))
                } else {
                    settingsDataManager.updateTwoFactor(Settings.AUTH_TYPE_OFF).firstOrError().map {
                        SecurityIntent.TwoFactorDisabled
                    }
                }
            }
        }

    fun updateScreenshotsEnabled(enabled: Boolean): Completable =
        Completable.fromAction { securityPrefs.setScreenshotsEnabled(enabled) }

    fun updateTor(blocked: Boolean): Single<Settings> = settingsDataManager.updateTor(blocked).firstOrError()

    fun checkBiometricsState(): SecurityIntent =
        if (!biometricsController.areBiometricsEnrolled) {
            SecurityIntent.UpdateViewState(SecurityViewState.ShowEnrollBiometrics)
        } else {
            val pin = pinRepository.pin
            if (pin.isNotEmpty()) {
                if (!biometricsController.isBiometricUnlockEnabled) {
                    SecurityIntent.UpdateViewState(SecurityViewState.ShowEnableBiometrics)
                } else {
                    SecurityIntent.EnableBiometrics
                }
            } else {
                SecurityIntent.UpdateErrorState(SecurityError.PIN_MISSING_EXCEPTION)
            }
        }

    fun disableBiometricLogin(): Completable =
        Completable.fromAction { biometricsController.setBiometricUnlockDisabled() }
}
