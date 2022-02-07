package piuk.blockchain.android.ui.home.models

import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class ActionsSheetIntent : MviIntent<ActionsSheetState> {

    object CheckForPendingBuys : ActionsSheetIntent() {
        override fun reduce(oldState: ActionsSheetState): ActionsSheetState = oldState
    }

    object CheckCtaOrdering : ActionsSheetIntent() {
        override fun reduce(oldState: ActionsSheetState): ActionsSheetState = oldState
    }

    class UpdateCtaOrdering(private val ordering: SplitButtonCtaOrdering) : ActionsSheetIntent() {
        override fun reduce(oldState: ActionsSheetState): ActionsSheetState = oldState.copy(
            splitButtonCtaOrdering = ordering
        )
    }

    class UpdateFlowToLaunch(private val flowToLaunch: FlowToLaunch) : ActionsSheetIntent() {
        override fun reduce(oldState: ActionsSheetState): ActionsSheetState =
            oldState.copy(
                flowToLaunch = flowToLaunch
            )
    }
}
