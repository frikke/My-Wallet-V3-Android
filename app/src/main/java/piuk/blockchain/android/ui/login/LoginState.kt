package piuk.blockchain.android.ui.login

import android.net.Uri
import com.blockchain.commonarch.presentation.mvi.MviState
import piuk.blockchain.android.ui.login.auth.LoginAuthInfo

enum class LoginStep {
    APP_MAINTENANCE,
    SELECT_METHOD,
    LOG_IN,
    SHOW_SCAN_ERROR,
    ENTER_PIN,
    REQUEST_APPROVAL,
    NAVIGATE_TO_LANDING_PAGE,
    ENTER_EMAIL,
    NAVIGATE_FROM_DEEPLINK,
    NAVIGATE_FROM_PAYLOAD,
    POLLING_PAYLOAD_ERROR,
    GET_SESSION_ID,
    SEND_EMAIL,
    VERIFY_DEVICE,
    MANUAL_PAIRING,
    SHOW_SESSION_ERROR,
    SHOW_EMAIL_ERROR,
    UNKNOWN_ERROR
}

data class LoginState(
    val email: String = "",
    val captcha: String = "",
    val sessionId: String = "",
    val currentStep: LoginStep = LoginStep.SELECT_METHOD,
    val shouldRestartApp: Boolean = false,
    val intentAction: String? = null,
    val intentUri: Uri? = null,
    val guid: String = "",
    val pollingState: AuthPollingState = AuthPollingState.NOT_STARTED,
    val payload: LoginAuthInfo.ExtendedAccountInfo? = null,
    val payloadBase64String: String = "",
    val loginApprovalState: LoginApprovalState = LoginApprovalState.NONE
) : MviState {
    val isLoading: Boolean
        get() = setOf(LoginStep.LOG_IN, LoginStep.GET_SESSION_ID, LoginStep.SEND_EMAIL).contains(currentStep)
    val isTypingEmail: Boolean
        get() = setOf(LoginStep.ENTER_EMAIL, LoginStep.SEND_EMAIL).contains(currentStep)
}

enum class AuthPollingState {
    NOT_STARTED,
    POLLING,
    SUSPENDED,
    TIMEOUT,
    COMPLETE,
    ERROR,
    DENIED
}

enum class LoginApprovalState {
    NONE,
    APPROVED,
    REJECTED
}
