package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.StakingAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.data.DataResource
import info.blockchain.balance.Money
import piuk.blockchain.android.ui.coinview.domain.LoadAssetAccountsUseCase

/**
 * Comparator is used to sort the accounts list in [LoadAssetAccountsUseCase]
 */
class CoinviewAccountDetail(
    val account: BlockchainAccount,
    val balance: DataResource<Money>,
    val isAvailable: Boolean,
    val isDefault: Boolean = false
) : Comparable<CoinviewAccountDetail> {
    override fun compareTo(other: CoinviewAccountDetail): Int {
        return getAssignedComparatorValue(this).compareTo(getAssignedComparatorValue(other))
    }

    private fun getAssignedComparatorValue(detailItem: CoinviewAccountDetail): Int {
        return when {
            detailItem.account is NonCustodialAccount && detailItem.isDefault -> 0
            detailItem.account is TradingAccount -> 1
            detailItem.account is InterestAccount -> 2
            detailItem.account is StakingAccount -> 3
            detailItem.account is NonCustodialAccount && detailItem.isDefault.not() -> 4
            else -> Int.MAX_VALUE
        }
    }
}
