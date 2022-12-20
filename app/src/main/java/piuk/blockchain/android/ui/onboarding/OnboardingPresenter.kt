package piuk.blockchain.android.ui.onboarding

import androidx.annotation.VisibleForTesting
import com.blockchain.core.access.PinRepository
import com.blockchain.core.settings.SettingsDataManager
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.base.BasePresenter

internal class OnboardingPresenter constructor(
    private val biometricsController: BiometricsController,
    private val pinRepository: PinRepository,
    private val settingsDataManager: SettingsDataManager
) : BasePresenter<OnboardingView>() {

    private val showEmail: Boolean by lazy { view.showEmail }
    private val showFingerprints: Boolean by lazy { view.showFingerprints }

    @VisibleForTesting
    internal var email: String? = null

    override fun onViewReady() {
        compositeDisposable += settingsDataManager.getSettings()
            .doAfterTerminate { this.checkAppState() }
            .subscribeBy(
                onNext = { settings -> email = settings.email },
                onError = { it.printStackTrace() }
            )
    }

    /**
     * Checks status of fingerprint hardware and either prompts the user to verify their fingerprint
     * or enroll one if the fingerprint sensor has never been set up.
     */
    internal fun onEnableFingerprintClicked() {
        if (biometricsController.isBiometricAuthEnabled) {
            val pin = pinRepository.pin

            if (pin.isNotEmpty()) {
                view.showFingerprintDialog(pin)
            } else {
                throw IllegalStateException("PIN not found")
            }
        } else if (biometricsController.isHardwareDetected) {
            // Hardware available but user has never set up fingerprints
            view.showEnrollFingerprintsDialog()
        } else {
            throw IllegalStateException("Fingerprint hardware not available, yet functions requiring hardware called.")
        }
    }

    /**
     * Sets fingerprint unlock enabled and clears the encrypted PIN if {@param enabled} is false
     *
     * @param enabled Whether or not the fingerprint unlock feature is set up
     */
    internal fun setFingerprintUnlockDisabled() {
        biometricsController.setBiometricUnlockDisabled()
    }

    private fun checkAppState() {
        when {
            showEmail -> view.showEmailPrompt()
            showFingerprints -> view.showFingerprintPrompt()
            else -> view.showEmailPrompt()
        }
    }
}
