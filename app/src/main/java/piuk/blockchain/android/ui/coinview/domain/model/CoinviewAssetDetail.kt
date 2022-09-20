package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.AssetFilter
import info.blockchain.balance.Money

sealed class CoinviewAssetDetail(
    open val isAddedToWatchlist: Boolean
) {
    data class Tradeable(
        override val isAddedToWatchlist: Boolean,
        val accounts: CoinviewAccounts,
        val totalBalance: CoinviewAssetTotalBalance
    ) : CoinviewAssetDetail(isAddedToWatchlist)

    class NonTradeable(
        override val isAddedToWatchlist: Boolean,
    ) : CoinviewAssetDetail(isAddedToWatchlist)
}

data class CoinviewAssetTotalBalance(
    val totalCryptoBalance: Map<AssetFilter, Money>,
    val totalFiatBalance: Money
)
