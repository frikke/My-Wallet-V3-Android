package piuk.blockchain.android.ui.login

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.blockchain.commonarch.presentation.mvi.MviIntent
import piuk.blockchain.android.ui.login.auth.LoginAuthInfo

sealed class LoginIntents : MviIntent<LoginState> {

    data class UpdateEmail(private val email: String) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                email = email,
                currentStep = if (email.isBlank()) {
                    LoginStep.SELECT_METHOD
                } else {
                    LoginStep.ENTER_EMAIL
                }
            )
    }

    data class ObtainSessionIdForEmail(
        val selectedEmail: String,
        val captcha: String
    ) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                email = selectedEmail,
                captcha = captcha,
                currentStep = LoginStep.GET_SESSION_ID
            )
    }

    data class SendEmail(
        val sessionId: String,
        val selectedEmail: String,
        val captcha: String
    ) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                email = selectedEmail,
                sessionId = sessionId,
                currentStep = LoginStep.SEND_EMAIL
            )
    }

    object ShowEmailSent : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.VERIFY_DEVICE
            )
    }

    object StartAuthPolling : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                pollingState = AuthPollingState.POLLING
            )
    }

    object AuthPollingTimeout : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.POLLING_PAYLOAD_ERROR,
                pollingState = AuthPollingState.TIMEOUT
            )
    }

    object AuthPollingError : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.POLLING_PAYLOAD_ERROR,
                pollingState = AuthPollingState.ERROR
            )
    }

    object RevertToEmailInput : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.ENTER_EMAIL,
                pollingState = AuthPollingState.NOT_STARTED
            )
    }

    object CancelPolling : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                pollingState = if (oldState.pollingState == AuthPollingState.POLLING) {
                    AuthPollingState.SUSPENDED
                } else {
                    AuthPollingState.NOT_STARTED
                }
            )
    }

    object ResumePolling : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                pollingState = if (oldState.pollingState == AuthPollingState.SUSPENDED) {
                    AuthPollingState.POLLING
                } else {
                    oldState.pollingState
                }
            )
    }

    object AuthPollingDenied : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.POLLING_PAYLOAD_ERROR,
                pollingState = AuthPollingState.DENIED
            )
    }

    object GetSessionIdFailed : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.SHOW_SESSION_ERROR
            )
    }

    object ShowEmailFailed : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.SHOW_EMAIL_ERROR
            )
    }

    object StartPinEntry : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.ENTER_PIN
            )
    }

    class StartPinEntryWithLoggingInPrompt(private val isLoginApproved: Boolean) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.ENTER_PIN,
                loginApprovalState = if (isLoginApproved) {
                    LoginApprovalState.APPROVED
                } else {
                    LoginApprovalState.REJECTED
                }
            )
    }

    class RevertToLauncher(private val isLoginApproved: Boolean) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.NAVIGATE_TO_LANDING_PAGE,
                loginApprovalState = if (isLoginApproved) {
                    LoginApprovalState.APPROVED
                } else {
                    LoginApprovalState.REJECTED
                }
            )
    }

    data class LoginWithQr(val qrString: String) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.LOG_IN
            )
    }

    data class ShowScanError(private val shouldRestartApp: Boolean) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.SHOW_SCAN_ERROR,
                shouldRestartApp = shouldRestartApp
            )
    }

    class CheckForExistingSessionOrDeepLink(val action: String, val uri: Uri) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState = oldState
    }

    object CheckShouldNavigateToOtherScreen : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(loginApprovalState = LoginApprovalState.NONE)
    }

    object UnknownError : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState = oldState.copy(currentStep = LoginStep.UNKNOWN_ERROR)
    }

    object UserLoggedInWithoutDeeplinkData : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.ENTER_PIN
            )
    }

    class ReceivedExternalLoginApprovalRequest(
        private val base64Payload: String,
        private val payload: LoginAuthInfo.ExtendedAccountInfo
    ) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.REQUEST_APPROVAL,
                payloadBase64String = base64Payload,
                payload = payload
            )
    }

    object ApproveLoginRequest : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState = oldState
    }

    object DenyLoginRequest : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState = oldState
    }

    object ResetState : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState = LoginState()
    }

    class UserAuthenticationRequired(
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val action: String?,
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val uri: Uri
    ) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.NAVIGATE_FROM_DEEPLINK,
                intentAction = action,
                intentUri = uri
            )
    }

    class PollingPayloadReceived(
        private val payload: LoginAuthInfo.ExtendedAccountInfo
    ) : LoginIntents() {
        override fun reduce(oldState: LoginState): LoginState =
            oldState.copy(
                currentStep = LoginStep.NAVIGATE_FROM_PAYLOAD,
                payload = payload,
                pollingState = AuthPollingState.COMPLETE
            )
    }
}
