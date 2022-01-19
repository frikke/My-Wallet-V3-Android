package piuk.blockchain.android.ui.auth.newlogin

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.preferences.BrowserIdentity

sealed class AuthNewLoginIntents : MviIntent<AuthNewLoginState> {
    data class InitAuthInfo(
        val pubKeyHash: String,
        val messageInJson: String,
        val originIp: String,
        private val items: List<AuthNewLoginDetailsType>,
        private val forcePin: Boolean
    ) : AuthNewLoginIntents() {
        override fun reduce(oldState: AuthNewLoginState): AuthNewLoginState {
            return oldState.copy(
                ip = originIp,
                items = items,
                forcePin = forcePin
            )
        }
    }

    data class ProcessBrowserMessage(
        val originIp: String,
        val browserIdentity: BrowserIdentity,
        val message: SecureChannelBrowserMessage
    ) : AuthNewLoginIntents() {
        override fun reduce(oldState: AuthNewLoginState): AuthNewLoginState =
            oldState.copy(
                items = oldState.items + AuthNewLoginLastLogin(message.timestamp),
                browserIdentity = browserIdentity,
                message = message,
                ip = originIp
            )
    }

    data class EnableApproval(
        private val enableApproval: Boolean,
        private val errorState: NewLoginState
    ) : AuthNewLoginIntents() {
        override fun reduce(oldState: AuthNewLoginState): AuthNewLoginState =
            oldState.copy(
                enableApproval = enableApproval,
                errorState = errorState
            )
    }

    object LoginApproved : AuthNewLoginIntents() {
        override fun reduce(oldState: AuthNewLoginState): AuthNewLoginState = oldState.copy()
    }

    object LoginDenied : AuthNewLoginIntents() {
        override fun reduce(oldState: AuthNewLoginState): AuthNewLoginState = oldState.copy()
    }
}
