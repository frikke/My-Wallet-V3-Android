package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.StateAwareAction
import org.junit.Test

class StateAwareActionsComparatorTest {

    private val comparator = StateAwareActionsComparator()

    @Test
    fun `comparator test`() {
        val actions = AssetAction.values().toList().shuffled().map {
            StateAwareAction(ActionState.Available, it)
        }

        val expected = listOf(
            StateAwareAction(ActionState.Available, AssetAction.Buy),
            StateAwareAction(ActionState.Available, AssetAction.Sell),
            StateAwareAction(ActionState.Available, AssetAction.Swap),
            StateAwareAction(ActionState.Available, AssetAction.Send),
            StateAwareAction(ActionState.Available, AssetAction.Receive),
            StateAwareAction(ActionState.Available, AssetAction.FiatDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.Withdraw),
            StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
            StateAwareAction(ActionState.Available, AssetAction.ViewActivity),
            StateAwareAction(ActionState.Available, AssetAction.Sign)
        )

        assert(actions.sortedWith(comparator) == expected)
    }

    @Test
    fun `comparator test 2`() {
        val expected = listOf(
            StateAwareAction(ActionState.Available, AssetAction.Buy),
            StateAwareAction(ActionState.Available, AssetAction.Swap),
            StateAwareAction(ActionState.Available, AssetAction.Send),
            StateAwareAction(ActionState.Available, AssetAction.Receive),
            StateAwareAction(ActionState.Available, AssetAction.Withdraw),
            StateAwareAction(ActionState.Available, AssetAction.ViewStatement)
        )

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }

    @Test
    fun `comparator test 3`() {
        val expected = listOf(
            StateAwareAction(ActionState.Available, AssetAction.Sell),
            StateAwareAction(ActionState.Available, AssetAction.FiatDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.Withdraw)
        )

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }

    @Test
    fun `comparator test 4`() {
        val expected = listOf(
            StateAwareAction(ActionState.Available, AssetAction.Buy),
            StateAwareAction(ActionState.Available, AssetAction.Sell),
            StateAwareAction(ActionState.Available, AssetAction.Swap),
            StateAwareAction(ActionState.Available, AssetAction.Send),
            StateAwareAction(ActionState.Available, AssetAction.Receive),
            StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
            StateAwareAction(ActionState.Available, AssetAction.ViewActivity)
        )

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }
}
