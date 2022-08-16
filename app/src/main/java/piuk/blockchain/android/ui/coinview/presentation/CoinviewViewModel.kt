package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

class CoinviewViewModel(
) : MviViewModel<
    CoinviewIntents,
    CoinviewViewState,
    CoinviewModelState,
    CoinviewNavigationEvent,
    ModelConfigArgs.NoArgs>(CoinviewModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: CoinviewModelState): CoinviewViewState {
        return CoinviewViewState()
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntents) {
    }
}
