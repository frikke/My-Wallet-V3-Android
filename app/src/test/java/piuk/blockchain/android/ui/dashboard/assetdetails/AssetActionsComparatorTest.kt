package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.coincore.AssetAction
import org.junit.Test

class AssetActionsComparatorTest {

    private val comparator = AssetActionsComparator()

    @Test
    fun `comparator test`() {
        val actions = AssetAction.values().toList().shuffled()

        val expected = listOf(
            AssetAction.Buy,
            AssetAction.Sell,
            AssetAction.Swap,
            AssetAction.Send,
            AssetAction.Receive,
            AssetAction.FiatDeposit,
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.Withdraw,
            AssetAction.ViewStatement,
            AssetAction.ViewActivity
        )

        assert(actions.sortedWith(comparator) == expected)
    }

    @Test
    fun `comparator test 2`() {
        val expected = listOf(
            AssetAction.Buy,
            AssetAction.Swap,
            AssetAction.Send,
            AssetAction.Receive,
            AssetAction.Withdraw,
            AssetAction.ViewStatement
        )

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }

    @Test
    fun `comparator test 3`() {
        val expected = listOf(
            AssetAction.Sell,
            AssetAction.FiatDeposit,
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.Withdraw
        )

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }

    @Test
    fun `comparator test 4`() {
        val expected = listOf(
            AssetAction.Buy,
            AssetAction.Sell,
            AssetAction.Swap,
            AssetAction.Send,
            AssetAction.Receive,
            AssetAction.ViewStatement,
            AssetAction.ViewActivity
        )

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }
}
