package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.coincore.AssetAction

class AssetActionsComparator : Comparator<AssetAction> {
    override fun compare(p0: AssetAction, p1: AssetAction): Int =
        p0.sortingValue().compareTo(p1.sortingValue())

    private fun AssetAction.sortingValue(): Int = when (this) {
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
    }

}