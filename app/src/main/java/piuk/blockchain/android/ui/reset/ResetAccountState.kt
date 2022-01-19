package piuk.blockchain.android.ui.reset

import com.blockchain.commonarch.presentation.mvi.MviState

enum class ResetAccountStatus {
    SHOW_INFO,
    RETRY,
    SHOW_WARNING,
    RESET
}

data class ResetAccountState(val status: ResetAccountStatus = ResetAccountStatus.SHOW_INFO) :
    MviState
