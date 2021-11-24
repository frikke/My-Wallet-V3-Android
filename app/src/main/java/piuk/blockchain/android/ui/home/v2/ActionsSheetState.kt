package piuk.blockchain.android.ui.home.v2

import piuk.blockchain.android.ui.base.mvi.MviState

data class ActionsSheetState(
    val flowToLaunch: FlowToLaunch = FlowToLaunch.None
) : MviState

sealed class FlowToLaunch {
    object None : FlowToLaunch()
    class TooManyPendingBuys(val maxTransactions: Int) : FlowToLaunch()
    object BuyFlow : FlowToLaunch()
}
