package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import info.blockchain.balance.Money

sealed interface CoinviewAccounts {
    val accounts: List<CoinviewAccount>

    data class Universal(
        override val accounts: List<CoinviewAccount.Universal>
    ) : CoinviewAccounts

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
    val cryptoBalance: Money
    val fiatBalance: Money

    /**
     * Universal mode
     * Should support all types of accounts: custodial, non custodial, interest
     */
    data class Universal(
        override val filter: AssetFilter,
        override val isEnabled: Boolean,
        override val account: BlockchainAccount,
        override val cryptoBalance: Money,
        override val fiatBalance: Money,
        val interestRate: Double,
        val stakingRate: Double
    ) : CoinviewAccount

    /**
     * Brokerage mode
     * also separates between custodial and interest accounts
     */
    sealed interface Custodial : CoinviewAccount {
        data class Trading(
            override val isEnabled: Boolean,
            override val account: BlockchainAccount,
            override val cryptoBalance: Money,
            override val fiatBalance: Money
        ) : Custodial {
            override val filter: AssetFilter = AssetFilter.Trading
        }

        data class Interest(
            override val isEnabled: Boolean,
            override val account: BlockchainAccount,
            override val cryptoBalance: Money,
            override val fiatBalance: Money,
            val interestRate: Double
        ) : Custodial {
            override val filter: AssetFilter = AssetFilter.Interest
        }

        data class Staking(
            override val isEnabled: Boolean,
            override val account: BlockchainAccount,
            override val cryptoBalance: Money,
            override val fiatBalance: Money,
            val stakingRate: Double
        ) : Custodial {
            override val filter: AssetFilter = AssetFilter.Staking
        }
    }

    /**
     * Defi mode
     */
    data class PrivateKey(
        override val account: BlockchainAccount,
        override val cryptoBalance: Money,
        override val fiatBalance: Money,
        override val isEnabled: Boolean
    ) : CoinviewAccount {
        override val filter: AssetFilter = AssetFilter.NonCustodial
    }
}
