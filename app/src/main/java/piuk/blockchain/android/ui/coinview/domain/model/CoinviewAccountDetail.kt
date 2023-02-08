package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.data.DataResource
import info.blockchain.balance.Money
import piuk.blockchain.android.ui.coinview.domain.LoadAssetAccountsUseCase

/**
 * Comparator is used to sort the accounts list in [LoadAssetAccountsUseCase]
 */
class CoinviewAccountDetail(
    val account: SingleAccount,
    val balance: DataResource<Money>,
    val isAvailable: Boolean,
) {
    fun getIndexedValue(): Int {
        return when {
            account is NonCustodialAccount && account.isDefault -> 0
            account is NonCustodialAccount -> 1
            account is TradingAccount -> 2
            account is EarnRewardsAccount.Interest -> 3
            account is EarnRewardsAccount.Staking -> 4
            account is EarnRewardsAccount.Active -> 5
            else -> Int.MAX_VALUE
        }
    }
}
