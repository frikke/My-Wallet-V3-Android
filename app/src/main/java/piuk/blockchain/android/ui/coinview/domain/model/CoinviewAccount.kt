package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import info.blockchain.balance.Money

sealed interface CoinviewAccounts {
    val accounts: List<CoinviewAccount>

    data class Custodial(
        override val accounts: List<CoinviewAccount.Custodial>
    ) : CoinviewAccounts

    data class Defi(
        override val accounts: List<CoinviewAccount.PrivateKey>
    ) : CoinviewAccounts
}

sealed interface CoinviewAccount {
    val filter: AssetFilter
    val isEnabled: Boolean
    val account: BlockchainAccount
    val cryptoBalance: DataResource<Money>
    val fiatBalance: DataResource<Money?>

    val isClickable: Boolean
        get() = cryptoBalance is DataResource.Data && fiatBalance is DataResource.Data

    /**
     * Brokerage mode
     * also separates between custodial and interest accounts
     */
    sealed interface Custodial : CoinviewAccount {
        data class Trading(
            override val isEnabled: Boolean,
            override val account: BlockchainAccount,
            override val cryptoBalance: DataResource<Money>,
            override val fiatBalance: DataResource<Money?>
        ) : Custodial {
            override val filter: AssetFilter = AssetFilter.Trading
        }

        data class Interest(
            override val isEnabled: Boolean,
            override val account: BlockchainAccount,
            override val cryptoBalance: DataResource<Money>,
            override val fiatBalance: DataResource<Money?>,
            val interestRate: Double
        ) : Custodial {
            override val filter: AssetFilter = AssetFilter.Interest
        }

        data class Staking(
            override val isEnabled: Boolean,
            override val account: BlockchainAccount,
            override val cryptoBalance: DataResource<Money>,
            override val fiatBalance: DataResource<Money?>,
            val stakingRate: Double
        ) : Custodial {
            override val filter: AssetFilter = AssetFilter.Staking
        }

        data class ActiveRewards(
            override val isEnabled: Boolean,
            override val account: BlockchainAccount,
            override val cryptoBalance: DataResource<Money>,
            override val fiatBalance: DataResource<Money?>,
            val activeRewardsRate: Double
        ) : Custodial {
            override val filter: AssetFilter = AssetFilter.ActiveRewards
        }
    }

    /**
     * Defi mode
     */
    data class PrivateKey(
        override val account: SingleAccount,
        override val cryptoBalance: DataResource<Money>,
        override val fiatBalance: DataResource<Money?>,
        override val isEnabled: Boolean,
        val address: String
    ) : CoinviewAccount {
        override val filter: AssetFilter = AssetFilter.NonCustodial
    }
}

fun CoinviewAccount.isTradingAccount(): Boolean {
    return this is CoinviewAccount.Custodial.Trading
}

fun CoinviewAccount.isInterestAccount(): Boolean {
    return this is CoinviewAccount.Custodial.Interest
}

fun CoinviewAccount.isStakingAccount(): Boolean {
    return this is CoinviewAccount.Custodial.Staking
}

fun CoinviewAccount.isActiveRewardsAccount(): Boolean {
    return this is CoinviewAccount.Custodial.ActiveRewards
}

fun CoinviewAccount.isPrivateKeyAccount(): Boolean {
    return this is CoinviewAccount.PrivateKey
}
