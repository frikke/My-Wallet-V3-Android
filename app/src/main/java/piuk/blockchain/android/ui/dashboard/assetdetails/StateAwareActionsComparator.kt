package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import info.blockchain.balance.Money

class StateAwareActionsComparator : Comparator<StateAwareAction> {
    private lateinit var accountForActions: BlockchainAccount
    private lateinit var accountBalance: Money

    fun initAccount(account: BlockchainAccount, balance: AccountBalance) {
        accountForActions = account
        accountBalance = balance.total
    }

    override fun compare(p0: StateAwareAction, p1: StateAwareAction): Int {
        require(::accountForActions.isInitialized)
        require(::accountBalance.isInitialized)

        return p0.sortingValue().compareTo(p1.sortingValue())
    }

    private fun StateAwareAction.sortingValue(): Int = when (accountForActions) {
        is CustodialTradingAccount -> this.action.tradingAccountsOrdering()
        is CustodialInterestAccount -> this.action.tradingAccountsOrdering()
        is CryptoNonCustodialAccount -> this.action.nonCustodialAccountOrdering()
        else -> throw IllegalStateException(
            "Trying to sort actions for an unsupported account type - $accountForActions"
        )
    }

    private fun AssetAction.tradingAccountsOrdering() = when (this) {
        AssetAction.Buy -> 0
        AssetAction.Sell -> 1
        AssetAction.Swap -> 2
        AssetAction.Send -> 3
        AssetAction.Receive -> 4
        AssetAction.FiatDeposit -> 5
        AssetAction.InterestDeposit -> 6
        AssetAction.InterestWithdraw -> 7
        AssetAction.FiatWithdraw -> 8
        AssetAction.ViewStatement -> 9
        AssetAction.ViewActivity -> 10
        AssetAction.Sign -> Int.MAX_VALUE
    }

    private fun AssetAction.nonCustodialAccountOrdering() = if (accountBalance.isPositive) {
        when (this) {
            AssetAction.Send -> 0
            AssetAction.Receive -> 1
            AssetAction.Swap -> 2
            AssetAction.Sell -> 3
            AssetAction.Buy -> 4
            AssetAction.FiatDeposit -> 5
            AssetAction.InterestDeposit -> 6
            AssetAction.InterestWithdraw -> 7
            AssetAction.FiatWithdraw -> 8
            AssetAction.ViewStatement -> 9
            AssetAction.ViewActivity -> 10
            AssetAction.Sign -> Int.MAX_VALUE
        }
    } else {
        when (this) {
            AssetAction.Receive -> 0
            AssetAction.Send -> 1
            AssetAction.Swap -> 2
            AssetAction.Sell -> 3
            AssetAction.Buy -> 4
            AssetAction.FiatDeposit -> 5
            AssetAction.InterestDeposit -> 6
            AssetAction.InterestWithdraw -> 7
            AssetAction.FiatWithdraw -> 8
            AssetAction.ViewStatement -> 9
            AssetAction.ViewActivity -> 10
            AssetAction.Sign -> Int.MAX_VALUE
        }
    }
}
