package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.walletmode.WalletMode
import piuk.blockchain.android.ui.coinview.domain.GetAccountActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickActions
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewRecurringBuys

/**
 * @property assetPriceHistory - contains chart data + price and price change information
 * @property interactiveAssetPrice - price and price change information, used when user is interacting with the chart
 */
data class CoinviewModelState(
    val walletMode: WalletMode,

    val asset: CryptoAsset? = null,

    // non tradeable asset
    val isNonTradeableAsset: Boolean = false,

    // price
    val isPriceDataLoading: Boolean = false,
    val isPriceDataError: Boolean = false,
    val assetPriceHistory: CoinviewAssetPriceHistory? = null,
    val requestedTimeSpan: HistoricalTimeSpan? = null,
    val interactiveAssetPrice: CoinviewAssetPrice? = null,

    // watchlist
    val isWatchlistLoading: Boolean = false,
    val isWatchlistError: Boolean = false,
    val watchlist: Boolean? = null,

    // total balance
    val isTotalBalanceLoading: Boolean = false,
    val isTotalBalanceError: Boolean = false,
    val totalBalance: CoinviewAssetTotalBalance? = null,

    // accounts
    val isAccountsLoading: Boolean = false,
    val isAccountsError: Boolean = false,
    val accounts: CoinviewAccounts? = null,

    // recurring buys
    val isRecurringBuysLoading: Boolean = false,
    val isRecurringBuysError: Boolean = false,
    val recurringBuys: CoinviewRecurringBuys? = null,

    // quick actions
    val isQuickActionsLoading: Boolean = false,
    val isQuickActionsError: Boolean = false,
    val quickActions: CoinviewQuickActions? = null,

    // asset info
    val isAssetInfoLoading: Boolean = false,
    val isAssetInfoError: Boolean = false,
    val assetInfo: DetailedAssetInformation? = null,

    // errors
    val error: CoinviewError = CoinviewError.None
) : ModelState {
    /**
     * Returns the first account that is:
     *
     * * Universal trading or defi
     *
     * *OR*
     *
     * * Cutodial trading (i.e. interest is not actionable)
     *
     * *OR*
     *
     * * Defi account
     *
     * *AND*
     *
     * * has a positive balance
     */
    val actionableAccount: CoinviewAccount
        get() {
            require(accounts != null) { "accounts not initialized" }

            return accounts.accounts.firstOrNull {
                val isUniversalTradingDefiAccount = it is CoinviewAccount.Universal &&
                    (it.filter == AssetFilter.Trading || it.filter == AssetFilter.NonCustodial)
                val isTradingAccount = it is CoinviewAccount.Custodial.Trading
                val isDefiAccount = it is CoinviewAccount.Defi
                val hasPositiveBalance = it.cryptoBalance.isPositive

                (isUniversalTradingDefiAccount || isTradingAccount || isDefiAccount) && hasPositiveBalance
            } ?: error("No actionable account found - maybe a quick action is active when it should be disabled")
        }
}

sealed interface CoinviewError {
    /**
     * Error that could occur when loading the account actions fails
     * @see GetAccountActionsUseCase
     */
    object ActionsLoadError : CoinviewError

    /**
     * Error that could occur when toggling watchlist fails
     */
    object WatchlistToggleError : CoinviewError

    object None : CoinviewError
}
