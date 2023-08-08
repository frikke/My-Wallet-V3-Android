package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import org.junit.Test

class StateAwareActionsComparatorTest {

    private val comparator = StateAwareActionsComparator()
    private val testAsset: CryptoCurrency = object : CryptoCurrency(
        displayTicker = "NOPE",
        networkTicker = "NOPE",
        name = "Not a real thing",
        categories = setOf(AssetCategory.TRADING),
        precisionDp = 8,
        requiredConfirmations = 3,
        colour = "000000"
    ) {}

    @Test
    fun `comparing when custodial account should return right order`() {
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
            StateAwareAction(ActionState.Available, AssetAction.StakingDeposit),
            StateAwareAction(ActionState.Available, AssetAction.ActiveRewardsDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.StakingWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.ActiveRewardsWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.FiatWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
            StateAwareAction(ActionState.Available, AssetAction.ViewActivity),
            StateAwareAction(ActionState.Available, AssetAction.Sign)
        )
        val account: CustodialTradingAccount = mock()
        val balance: AccountBalance = mock {
            on { total }.thenReturn(Money.zero(testAsset))
        }
        comparator.initAccount(account, balance)
        assert(actions.sortedWith(comparator) == expected)
    }

    @Test
    fun `comparing when interest account should return right order`() {
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
            StateAwareAction(ActionState.Available, AssetAction.StakingDeposit),
            StateAwareAction(ActionState.Available, AssetAction.ActiveRewardsDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.StakingWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.ActiveRewardsWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.FiatWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
            StateAwareAction(ActionState.Available, AssetAction.ViewActivity),
            StateAwareAction(ActionState.Available, AssetAction.Sign)
        )
        val account: CustodialInterestAccount = mock()
        val balance: AccountBalance = mock {
            on { total }.thenReturn(Money.zero(testAsset))
        }
        comparator.initAccount(account, balance)
        assert(actions.sortedWith(comparator) == expected)
    }

    @Test
    fun `comparing when non custodial account without balance should return order`() {
        val account: CryptoNonCustodialAccount = mock()
        val balance: AccountBalance = mock {
            on { total }.thenReturn(Money.zero(testAsset))
        }
        val expected = listOf(
            StateAwareAction(ActionState.Available, AssetAction.Receive),
            StateAwareAction(ActionState.Available, AssetAction.Send),
            StateAwareAction(ActionState.Available, AssetAction.Swap),
            StateAwareAction(ActionState.Available, AssetAction.Sell),
            StateAwareAction(ActionState.Available, AssetAction.Buy),
            StateAwareAction(ActionState.Available, AssetAction.FiatDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestDeposit),
            StateAwareAction(ActionState.Available, AssetAction.StakingDeposit),
            StateAwareAction(ActionState.Available, AssetAction.ActiveRewardsDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.FiatWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
            StateAwareAction(ActionState.Available, AssetAction.ViewActivity),
            StateAwareAction(ActionState.Available, AssetAction.Sign)
        )
        comparator.initAccount(account, balance)

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }

    @Test
    fun `comparing when non custodial account with balance should return order`() {
        val account: CryptoNonCustodialAccount = mock()
        val moneyBalance: Money = mock {
            on { isPositive }.thenReturn(true)
        }
        val balance: AccountBalance = mock {
            on { total }.thenReturn(moneyBalance)
        }

        val expected = listOf(
            StateAwareAction(ActionState.Available, AssetAction.Send),
            StateAwareAction(ActionState.Available, AssetAction.Receive),
            StateAwareAction(ActionState.Available, AssetAction.Swap),
            StateAwareAction(ActionState.Available, AssetAction.Sell),
            StateAwareAction(ActionState.Available, AssetAction.Buy),
            StateAwareAction(ActionState.Available, AssetAction.FiatDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestDeposit),
            StateAwareAction(ActionState.Available, AssetAction.StakingDeposit),
            StateAwareAction(ActionState.Available, AssetAction.ActiveRewardsDeposit),
            StateAwareAction(ActionState.Available, AssetAction.InterestWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.FiatWithdraw),
            StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
            StateAwareAction(ActionState.Available, AssetAction.ViewActivity),
            StateAwareAction(ActionState.Available, AssetAction.Sign)
        )
        comparator.initAccount(account, balance)

        assert(expected.shuffled().sortedWith(comparator) == expected)
    }
}
