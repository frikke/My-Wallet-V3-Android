package piuk.blockchain.android.ui.login

import android.net.Uri
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.login.auth.LoginAuthInfo

enum class LoginStep {
    SELECT_METHOD,
    LOG_IN,
    SHOW_SCAN_ERROR,
    ENTER_PIN,
    ENTER_EMAIL,
    NAVIGATE_FROM_DEEPLINK,
    NAVIGATE_FROM_PAYLOAD,
    POLLING_PAYLOAD_ERROR,
    GET_SESSION_ID,
    SEND_EMAIL,
    VERIFY_DEVICE,
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
    val pollingState: AuthPollingState = AuthPollingState.NOT_STARTED,
    val payload: LoginAuthInfo.ExtendedAccountInfo? = null
) : MviState {
    val isLoading: Boolean
        get() = setOf(LoginStep.LOG_IN, LoginStep.GET_SESSION_ID, LoginStep.SEND_EMAIL).contains(currentStep)
    val isTypingEmail: Boolean
        get() = setOf(LoginStep.ENTER_EMAIL, LoginStep.SEND_EMAIL).contains(currentStep)
}

enum class AuthPollingState {
    NOT_STARTED,
    POLLING,
    TIMEOUT,
    COMPLETE,
    ERROR,
    DENIED
}
