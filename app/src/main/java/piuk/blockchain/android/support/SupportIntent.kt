package piuk.blockchain.android.support

import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class SupportIntent : MviIntent<SupportState> {

    object LoadUserInfo : SupportIntent() {
        override fun reduce(oldState: SupportState): SupportState = oldState.copy(
            viewState = SupportViewState.Loading
        )
    }

    class UpdateViewState(private val supportViewState: SupportViewState) :
        SupportIntent() {
        override fun reduce(oldState: SupportState): SupportState = oldState.copy(
            viewState = supportViewState
        )
    }

    class OnTopicSelected(private val topic: String) : SupportIntent() {
        override fun reduce(oldState: SupportState): SupportState =
            oldState.copy(viewState = SupportViewState.TopicSelected(topic))
    }

    class UpdateErrorState(private val error: SupportError) :
        SupportIntent() {
        override fun reduce(oldState: SupportState): SupportState = oldState.copy(
            supportError = error
        )
    }

    object ResetViewState : SupportIntent() {
        override fun reduce(oldState: SupportState): SupportState = oldState.copy(viewState = SupportViewState.None)
    }

    object ResetErrorState : SupportIntent() {
        override fun reduce(oldState: SupportState): SupportState =
            oldState.copy(supportError = SupportError.None, viewState = SupportViewState.None)
    }
}
