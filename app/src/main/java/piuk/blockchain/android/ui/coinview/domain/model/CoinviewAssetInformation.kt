package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.AssetFilter
import info.blockchain.balance.Money

sealed class CoinviewAssetInformation(
    open val isAddedToWatchlist: Boolean
) {
    data class AccountsInfo(
        override val isAddedToWatchlist: Boolean,
        val accounts: CoinviewAccounts,
        val totalBalance: CoinviewAssetTotalBalance
    ) : CoinviewAssetInformation(isAddedToWatchlist)

    class NonTradeable(
        override val isAddedToWatchlist: Boolean,
    ) : CoinviewAssetInformation(isAddedToWatchlist)
}

data class CoinviewAssetTotalBalance(
    val totalCryptoBalance: Map<AssetFilter, Money>,
    val totalFiatBalance: Money
)
