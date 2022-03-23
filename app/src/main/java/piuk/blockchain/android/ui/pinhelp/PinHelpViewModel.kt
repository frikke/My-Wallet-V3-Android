package piuk.blockchain.android.ui.pinhelp

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

class PinHelpViewModel(
    loginHelpModelState: PinHelpModelState
) : MviViewModel<PinHelpIntents, PinHelpViewState, PinHelpModelState, PinHelpNavigationEvent, ModelConfigArgs.NoArgs>(
    loginHelpModelState
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: PinHelpModelState): PinHelpViewState {
        return PinHelpViewState
    }

    override suspend fun handleIntent(modelState: PinHelpModelState, intent: PinHelpIntents) {
    }
}

