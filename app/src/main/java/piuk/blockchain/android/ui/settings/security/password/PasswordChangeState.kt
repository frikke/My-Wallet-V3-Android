package piuk.blockchain.android.ui.settings.security.password

import com.blockchain.commonarch.presentation.mvi.MviState

data class PasswordChangeState(
    val errorState: PasswordChangeError = PasswordChangeError.NONE,
    val passwordViewState: PasswordViewState = PasswordViewState.None
) : MviState

sealed class PasswordViewState {
    object None : PasswordViewState()
    object PasswordUpdated : PasswordViewState()
    object CheckingPasswords : PasswordViewState()
}

enum class PasswordChangeError {
    NONE,
    USING_SAME_PASSWORDS,
    CURRENT_PASSWORD_WRONG,
    NEW_PASSWORDS_DONT_MATCH,
    NEW_PASSWORD_INVALID_LENGTH,
    NEW_PASSWORD_TOO_WEAK,
    UNKNOWN_ERROR
}
