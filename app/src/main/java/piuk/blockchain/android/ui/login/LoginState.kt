package piuk.blockchain.android.ui.login

import android.net.Uri
import piuk.blockchain.android.ui.base.mvi.MviState

enum class LoginStep {
    SELECT_METHOD,
    LOG_IN,
    SHOW_SCAN_ERROR,
    ENTER_PIN,
    ENTER_EMAIL,
    NAVIGATE_FROM_DEEPLINK,
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
    val intentUri: Uri? = null
) : MviState {
    val isLoading: Boolean
        get() = setOf(LoginStep.LOG_IN, LoginStep.GET_SESSION_ID, LoginStep.SEND_EMAIL).contains(currentStep)
    val isTypingEmail: Boolean
        get() = setOf(LoginStep.ENTER_EMAIL, LoginStep.SEND_EMAIL).contains(currentStep)
}
