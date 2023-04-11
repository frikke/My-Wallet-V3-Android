package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

class EnterAmountViewModel(
) : MviViewModel<EnterAmountIntent, EnterAmountViewState, EnterAmountModelState, EmptyNavEvent, ModelConfigArgs.NoArgs>(
    EnterAmountModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: EnterAmountModelState): EnterAmountViewState {
        return with(state) {
            EnterAmountViewState(
                fromAsset = "",
                toAsset = ""
            )
        }
    }

    override suspend fun handleIntent(modelState: EnterAmountModelState, intent: EnterAmountIntent) {
        when (intent) {
            EnterAmountIntent.LoadData -> {
            }
        }
    }
}
