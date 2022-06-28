package piuk.blockchain.android.ui.settings.v2.security

import com.blockchain.commonarch.presentation.mvi.MviState

data class SecurityState(
    val securityInfo: SecurityInfo? = null,
    val errorState: SecurityError = SecurityError.NONE,
    val securityViewState: SecurityViewState = SecurityViewState.None
) : MviState

sealed class SecurityViewState {
    object None : SecurityViewState()
    object ConfirmBiometricsDisabling : SecurityViewState()
    object ShowEnrollBiometrics : SecurityViewState()
    object ShowEnableBiometrics : SecurityViewState()
    object ShowEnterPhoneNumberRequired : SecurityViewState()
    class ShowVerifyPhoneNumberRequired(val phoneNumber: String) : SecurityViewState()
    object ShowDisablingOnWebRequired : SecurityViewState()
    object ShowConfirmTwoFaEnabling : SecurityViewState()
    object LaunchPasswordChange : SecurityViewState()
    object ShowMustBackWalletUp : SecurityViewState()
}

enum class SecurityError {
    NONE,
    LOAD_INITIAL_INFO_FAIL,
    PIN_MISSING_EXCEPTION,
    BIOMETRICS_DISABLING_FAIL,
    TWO_FA_TOGGLE_FAIL,
    TOR_FILTER_UPDATE_FAIL,
    SCREENSHOT_UPDATE_FAIL
}

data class SecurityInfo(
    val isBiometricsVisible: Boolean,
    val isBiometricsEnabled: Boolean,
    val isTorFilteringEnabled: Boolean,
    val areScreenshotsEnabled: Boolean,
    val isTwoFaEnabled: Boolean,
    val isWalletBackedUp: Boolean,
    val isCloudBackupEnabled: Boolean
)
