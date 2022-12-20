package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.commonarch.presentation.mvi.MviState
import com.google.android.play.core.appupdate.AppUpdateInfo
import piuk.blockchain.android.ui.auth.MobileNoticeDialog

data class PinState(
    val isLoading: Boolean = false,
    val error: PinError = PinError.NONE,
    val isApiHealthyStatus: Boolean = true,
    val action: PinScreenView = PinScreenView.LoginWithPin,
    val showMobileNotice: MobileNoticeDialog? = null,
    val passwordStatus: PasswordStatus? = null,
    val appUpgradeStatus: AppUpgradeStatus = AppUpgradeStatus(),
    val progressDialog: ProgressDialogStatus? = null,
    val biometricStatus: BiometricStatus = BiometricStatus(),
    val upgradeWalletStatus: UpgradeWalletStatus? = null,
    val payloadStatus: PayloadStatus = PayloadStatus(),
    val pinStatus: PinStatus = PinStatus(),
    val isIntercomEnabled: Boolean = false
) : MviState

sealed class PinScreenView {
    object CreateNewPin : PinScreenView()
    object ConfirmNewPin : PinScreenView()
    object LoginWithPin : PinScreenView()
}

data class PinStatus(
    val currentPin: String = "",
    val isPinValidated: Boolean = false,
    val isFromPinCreation: Boolean = false
)

data class BiometricStatus(
    val shouldShowFingerprint: Boolean = false,
    val canShowFingerprint: Boolean = true
)

data class UpgradeWalletStatus(
    val upgradeAppSucceeded: Boolean = false,
    val isWalletUpgradeRequired: Boolean = false
)

data class ProgressDialogStatus(
    val hasToShow: Boolean = false,
    val messageToShow: Int = 0
)

data class AppUpgradeStatus(
    val appNeedsToUpgrade: UpgradeAppMethod = UpgradeAppMethod.NONE,
    val appUpdateInfo: AppUpdateInfo? = null
)

data class PasswordStatus(
    val isPasswordValid: Boolean = false,
    val passwordError: PasswordError = PasswordError.NONE,
    val passwordTriesRemaining: Int = 0
)

data class PayloadStatus(
    val isPayloadCompleted: Boolean = false,
    val payloadError: PayloadError = PayloadError.NONE
)

enum class UpgradeAppMethod {
    FLEXIBLE,
    FORCED_NATIVELY,
    FORCED_STORE,
    NONE
}

enum class PinError {
    ERROR_CONNECTION,
    CREATE_PIN_FAILED,
    NUM_ATTEMPTS_EXCEEDED,
    DONT_MATCH,
    CHANGE_TO_EXISTING_PIN,
    INVALID_CREDENTIALS,
    ZEROS_PIN,
    PIN_INCOMPLETE,
    NONE
}

enum class PasswordError {
    SERVER_CONNECTION_EXCEPTION,
    SERVER_TIMEOUT,
    HD_WALLET_EXCEPTION,
    ACCOUNT_LOCKED,
    UNKNOWN,
    NONE
}

enum class PayloadError {
    CREDENTIALS_INVALID,
    SERVER_CONNECTION_EXCEPTION,
    SERVER_TIMEOUT,
    UNSUPPORTED_VERSION_EXCEPTION,
    DECRYPTION_EXCEPTION,
    HD_WALLET_EXCEPTION,
    INVALID_CIPHER_TEXT,
    ACCOUNT_LOCKED,
    UNKNOWN,
    NONE
}
