package piuk.blockchain.android.ui.settings.v2.security.pin

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.google.android.play.core.appupdate.AppUpdateManager
import piuk.blockchain.android.ui.auth.MobileNoticeDialog

sealed class PinIntent : MviIntent<PinState> {

    object CheckApiStatus : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class UpdateApiStatus(val apiStatus: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(isApiHealthyStatus = apiStatus)
    }

    object GetAction : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class UpdateAction(val pinScreenView: PinScreenView) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(action = pinScreenView)
    }

    data class CreatePIN(val pin: String) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                isLoading = true
            )
    }

    object CreatePINSucceeded : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                isLoading = false
            )
    }

    data class ValidatePIN(
        val pin: String,
        val isForValidatingPinForResult: Boolean = false,
        val isChangingPin: Boolean = false
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    object ValidatePINSucceeded : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                isLoading = false,
                pinStatus = PinStatus(
                    isPinValidated = true,
                    currentPin = oldState.pinStatus.currentPin,
                    isFromPinCreation = oldState.pinStatus.isFromPinCreation
                ),
            )
    }

    data class ValidatePINFailed(val pinError: PinError) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                isLoading = false,
                error = pinError
            )
    }

    class UpdatePinErrorState(val errorState: PinError) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                isLoading = false,
                error = errorState
            )
    }

    class UpdatePasswordErrorState(
        private val isValidPassword: Boolean = false,
        val errorState: PasswordError = PasswordError.NONE
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                passwordStatus = PasswordStatus(
                    isPasswordValid = isValidPassword,
                    passwordError = errorState
                )
            )
    }

    data class UpdatePayload(val password: String, val isFromPinCreation: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class PayloadSucceeded(val isFromPinCreation: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = true,
                    payloadError = PayloadError.NONE
                ),
                pinStatus = PinStatus(
                    isFromPinCreation = isFromPinCreation,
                    currentPin = oldState.pinStatus.currentPin,
                    isPinValidated = oldState.pinStatus.isPinValidated
                ),
                isLoading = false
            )
    }

    class UpdatePayloadErrorState(val errorState: PayloadError) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = errorState
                )
            )
    }

    object CheckNumPinAttempts : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    object FetchRemoteMobileNotice : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class ShowMobileNoticeDialog(val mobileDialog: MobileNoticeDialog) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(showMobileNotice = mobileDialog)
    }

    object CheckFingerprint : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class SetCanShowFingerprint(
        val canShow: Boolean
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                biometricStatus = BiometricStatus(
                    isBiometricsEnabled = oldState.biometricStatus.isBiometricsEnabled,
                    shouldShowFingerprint = oldState.biometricStatus.shouldShowFingerprint,
                    canShowFingerprint = canShow
                )
            )
    }

    data class SetFingerprintEnabled(
        val isEnabled: Boolean
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                biometricStatus = BiometricStatus(
                    isBiometricsEnabled = isEnabled,
                    shouldShowFingerprint = oldState.biometricStatus.shouldShowFingerprint,
                    canShowFingerprint = oldState.biometricStatus.canShowFingerprint
                )
            )
    }

    data class SetShowFingerprint(
        val shouldShow: Boolean
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                biometricStatus = BiometricStatus(
                    isBiometricsEnabled = oldState.biometricStatus.isBiometricsEnabled,
                    shouldShowFingerprint = shouldShow,
                    canShowFingerprint = oldState.biometricStatus.canShowFingerprint
                )
            )
    }

    data class UpgradeWallet(val secondPassword: String?, val isFromPinCreation: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class UpgradeWalletResponse(val hasSucceeded: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                isLoading = false,
                upgradeWalletStatus = UpgradeWalletStatus(
                    isWalletUpgradeRequired = oldState.upgradeWalletStatus?.isWalletUpgradeRequired ?: false,
                    upgradeAppSucceeded = hasSucceeded
                )
            )
    }

    data class UpgradeRequired(
        val isUpgradeRequired: Boolean,
        val isFromPinCreation: Boolean,
        val passwordTriesRemaining: Int
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                upgradeWalletStatus = UpgradeWalletStatus(
                    isWalletUpgradeRequired = isUpgradeRequired,
                    upgradeAppSucceeded = oldState.upgradeWalletStatus?.isWalletUpgradeRequired ?: false
                ),
                pinStatus = PinStatus(
                    isFromPinCreation = isFromPinCreation,
                    currentPin = oldState.pinStatus.currentPin,
                    isPinValidated = oldState.pinStatus.isPinValidated
                ),
                passwordStatus = PasswordStatus(
                    passwordTriesRemaining = passwordTriesRemaining,
                    isPasswordValid = oldState.passwordStatus?.isPasswordValid ?: false,
                    passwordError = oldState.passwordStatus?.passwordError ?: PasswordError.NONE
                )
            )
    }

    object ClearStateAlreadyHandled : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                error = PinError.NONE,
                passwordStatus = null,
                payloadStatus = PayloadStatus(),
                upgradeWalletStatus = null,
                progressDialog = null
            )
    }

    object GetCurrentPin : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class SetCurrentPin(val newPin: String) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                pinStatus = PinStatus(
                    currentPin = newPin,
                    isPinValidated = oldState.pinStatus.isPinValidated,
                    isFromPinCreation = oldState.pinStatus.isFromPinCreation
                )
            )
    }

    data class ValidatePassword(val password: String) : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class CheckAppUpgradeStatus(
        val versionName: String,
        val appUpdateManager: AppUpdateManager
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class AppNeedsUpgrade(val appStatus: AppUpgradeStatus) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                appUpgradeStatus = appStatus
            )
    }

    object PinLogout : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class HandleProgressDialog(val showDialog: Boolean, val msgResource: Int = 0) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                progressDialog = ProgressDialogStatus(
                    hasToShow = showDialog,
                    messageToShow = msgResource
                )
            )
    }
}
