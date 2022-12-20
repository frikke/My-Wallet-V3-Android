package piuk.blockchain.android.ui.settings.security.password

import androidx.annotation.VisibleForTesting
import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class PasswordChangeIntent : MviIntent<PasswordChangeState> {

    class UpdatePassword(val currentPassword: String, val newPassword: String, val newPasswordConfirmation: String) :
        PasswordChangeIntent() {
        override fun reduce(oldState: PasswordChangeState): PasswordChangeState = oldState.copy(
            passwordViewState = PasswordViewState.CheckingPasswords
        )
    }

    class UpdateErrorState(
        @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val errorState: PasswordChangeError
    ) : PasswordChangeIntent() {
        override fun reduce(oldState: PasswordChangeState): PasswordChangeState =
            oldState.copy(errorState = errorState)
    }

    class UpdateViewState(
        @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val viewState: PasswordViewState
    ) : PasswordChangeIntent() {
        override fun reduce(oldState: PasswordChangeState): PasswordChangeState =
            oldState.copy(passwordViewState = viewState)
    }

    object ResetViewState : PasswordChangeIntent() {
        override fun reduce(oldState: PasswordChangeState): PasswordChangeState =
            oldState.copy(passwordViewState = PasswordViewState.None)
    }

    object ResetErrorState : PasswordChangeIntent() {
        override fun reduce(oldState: PasswordChangeState): PasswordChangeState =
            oldState.copy(errorState = PasswordChangeError.NONE)
    }
}
