package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import info.blockchain.balance.Money

sealed class CoinviewAccount(
    open val account: BlockchainAccount,
    open val amount: Money,
    open val fiatValue: Money,
    open val interestRate: Double,
    open val filter: AssetFilter
) {
    data class Brokerage(
        override val account: BlockchainAccount,
        override val amount: Money,
        override val fiatValue: Money,
        override val interestRate: Double,
        override val filter: AssetFilter
    ) : CoinviewAccount(
        account, amount, fiatValue, interestRate, filter
    )

    data class Defi(
        override val account: BlockchainAccount,
        override val amount: Money,
        override val fiatValue: Money,
    ) : CoinviewAccount(
        account, amount, fiatValue, Double.NaN, AssetFilter.NonCustodial
    )
}