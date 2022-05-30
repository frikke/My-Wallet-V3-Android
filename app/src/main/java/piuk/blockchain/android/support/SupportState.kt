package piuk.blockchain.android.support

import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.nabu.BasicProfileInfo

data class SupportState(
    val viewState: SupportViewState = SupportViewState.None,
    val supportError: SupportError = SupportError.None,
    val crashErrorCount: Int = 0
) : MviState

sealed class SupportViewState {
    object None : SupportViewState()
    object Loading : SupportViewState()
    class TopicSelected(val topic: String) : SupportViewState()
    class ShowInfo(val userInfo: UserInfo) : SupportViewState()
}

enum class SupportError {
    None,
    ErrorLoadingProfileInfo,
    ErrorStartingChat
}

data class UserInfo(
    val isUserGold: Boolean,
    val basicInfo: BasicProfileInfo,
    val isIntercomEnabled: Boolean
)
