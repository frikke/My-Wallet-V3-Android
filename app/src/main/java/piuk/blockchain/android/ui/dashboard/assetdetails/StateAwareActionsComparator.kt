package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.StateAwareAction

class StateAwareActionsComparator : Comparator<StateAwareAction> {
    override fun compare(p0: StateAwareAction, p1: StateAwareAction): Int =
        p0.sortingValue().compareTo(p1.sortingValue())

    private fun StateAwareAction.sortingValue(): Int = when (this.action) {
        AssetAction.Buy -> 0
        AssetAction.Sell -> 1
        AssetAction.Swap -> 2
        AssetAction.Send -> 3
        AssetAction.Receive -> 4
        AssetAction.FiatDeposit -> 5
        AssetAction.InterestDeposit -> 6
        AssetAction.InterestWithdraw -> 7
        AssetAction.Withdraw -> 8
        AssetAction.ViewStatement -> 9
        AssetAction.ViewActivity -> 10
        AssetAction.Sign -> Int.MAX_VALUE
    }
}
