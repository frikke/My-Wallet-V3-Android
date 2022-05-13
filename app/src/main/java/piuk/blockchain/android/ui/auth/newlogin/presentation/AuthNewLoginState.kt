package piuk.blockchain.android.ui.auth.newlogin.presentation

import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.preferences.BrowserIdentity
import piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginDetailsType
import piuk.blockchain.android.ui.auth.newlogin.domain.model.SecureChannelBrowserMessage

data class AuthNewLoginState(
    val browserIdentity: BrowserIdentity = BrowserIdentity(""),
    val message: SecureChannelBrowserMessage = SecureChannelBrowserMessage("", "", 0),
    val items: List<AuthNewLoginDetailsType> = listOf(),
    val location: String = "",
    val ip: String = "",
    val info: String = "",
    val forcePin: Boolean = false,
    val enableApproval: Boolean = false,
    val errorState: NewLoginState = NewLoginState.NONE
) : MviState

enum class NewLoginState {
    NONE,
    IP_MISMATCH
}
