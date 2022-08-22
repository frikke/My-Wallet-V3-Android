package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.AssetFilter
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.Money

sealed class CoinviewAssetInformation(
    open val prices: Prices24HrWithDelta,
    open val isAddedToWatchlist: Boolean
) {
    data class AccountsInfo(
        override val isAddedToWatchlist: Boolean,
        override val prices: Prices24HrWithDelta,
        val accounts: CoinviewAccounts,
        val totalBalance: CoinviewAssetTotalBalance
    ) : CoinviewAssetInformation(prices, isAddedToWatchlist)

    class NonTradeable(
        override val isAddedToWatchlist: Boolean,
        override val prices: Prices24HrWithDelta,
    ) : CoinviewAssetInformation(prices, isAddedToWatchlist)
}

data class CoinviewAssetTotalBalance(
    val totalCryptoBalance: Map<AssetFilter, Money>,
    val totalFiatBalance: Money
)