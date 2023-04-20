package com.blockchain.transactions.swap.selecttarget

import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.transactions.swap.SwapService
import info.blockchain.balance.AssetCatalogue

class SelectTargetViewModel(
    private val swapService: SwapService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<SelectTargetIntent,
    SelectTargetViewState,
    SelectTargetModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs>(
    SelectTargetModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: SelectTargetModelState) = state.run {
        SelectTargetViewState()
    }

    override suspend fun handleIntent(modelState: SelectTargetModelState, intent: SelectTargetIntent) {
        when (intent) {
            is SelectTargetIntent.LoadData -> {
            }
        }
    }
}
