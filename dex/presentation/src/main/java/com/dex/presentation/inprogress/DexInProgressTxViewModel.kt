package com.dex.presentation.inprogress

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import info.blockchain.balance.AssetInfo

class DexInProgressTxViewModel(private val txProcessor: DexTransactionProcessor) : MviViewModel<
    InProgressIntent,
    InProgressViewState,
    InProgressModelState,
    InProgressNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = InProgressModelState(
        transaction = null,
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: InProgressModelState): InProgressViewState {
        TODO("Not yet implemented")
    }

    override suspend fun handleIntent(modelState: InProgressModelState, intent: InProgressIntent) {
        TODO("Not yet implemented")
    }
}

data class InProgressModelState(
    val transaction: DexTransaction?
) : ModelState

object InProgressNavigationEvent : NavigationEvent
sealed class InProgressViewState : ViewState {
    data class Success(
        val sourceCurrency: AssetInfo,
        val destinationCurrency: AssetInfo
    ) : InProgressViewState()

    object Failure : InProgressViewState()
}

sealed class InProgressIntent : Intent<InProgressModelState> {
    object LoadTransactionProgress : InProgressIntent()
}