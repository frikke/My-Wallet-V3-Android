package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.commonarch.presentation.mvi.MviIntent
import piuk.blockchain.android.ui.auth.MobileNoticeDialog

sealed class PinIntent : MviIntent<PinState> {

    object CheckApiStatus : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class UpdateApiStatus(val apiStatus: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(isApiHealthyStatus = apiStatus)
    }

    data class UpdateLoading(private val loading: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState {
            return oldState.copy(isLoading = loading)
        }
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

    object DialogShown : PinIntent() {
        override fun reduce(oldState: PinState): PinState {
            return oldState.copy(
                biometricStatus = oldState.biometricStatus.copy(
                    shouldShowFingerprint = false
                )
            )
        }
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
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = false,
                    canShowFingerprint = oldState.biometricStatus.canShowFingerprint
                )
            )
    }

    object ValidatePINSucceeded : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                pinStatus = PinStatus(
                    isPinValidated = true,
                    currentPin = oldState.pinStatus.currentPin,
                    isFromPinCreation = oldState.pinStatus.isFromPinCreation
                )
            )
    }

    object DisableBiometrics : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class ValidatePINFailed(val pinError: PinError) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                error = pinError
            )
    }

    class UpdatePinErrorState(val errorState: PinError) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
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

    object CheckIntercomStatus : PinIntent() {
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
                    shouldShowFingerprint = oldState.biometricStatus.shouldShowFingerprint,
                    canShowFingerprint = canShow
                )
            )
    }

    data class SetShowFingerprint(
        val shouldShow: Boolean
    ) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                biometricStatus = BiometricStatus(
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

    data class AppNeedsUpgrade(val appStatus: AppUpgradeStatus) : PinIntent() {
        override fun reduce(oldState: PinState): PinState =
            oldState.copy(
                appUpgradeStatus = appStatus
            )
    }

    object PinLogout : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState
    }

    data class UpdateIntercomStatus(private val isIntercomEnabled: Boolean) : PinIntent() {
        override fun reduce(oldState: PinState): PinState = oldState.copy(isIntercomEnabled = isIntercomEnabled)
    }
}
