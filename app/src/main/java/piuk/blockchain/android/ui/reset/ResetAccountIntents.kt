package piuk.blockchain.android.ui.reset

import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class ResetAccountIntents : MviIntent<ResetAccountState> {
    data class UpdateStatus(private val status: ResetAccountStatus) : ResetAccountIntents() {
        override fun reduce(oldState: ResetAccountState): ResetAccountState =
            oldState.copy(
                status = status
            )
    }
}
