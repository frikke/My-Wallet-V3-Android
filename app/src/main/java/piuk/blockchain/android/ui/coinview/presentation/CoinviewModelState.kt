package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.news.NewsArticle
import com.blockchain.walletmode.WalletMode
import piuk.blockchain.android.ui.coinview.domain.GetAccountActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetDetail
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickActions
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewRecurringBuys

/**
 * @property assetPriceHistory - contains chart data + price and price change information
 * @property interactiveAssetPrice - price and price change information, used when user is interacting with the chart
 */
data class CoinviewModelState(
    val walletMode: WalletMode? = null,

    val asset: CryptoAsset? = null,

    // kyc rejected
    val isKycRejected: Boolean = false,

    // price
    val isChartDataLoading: Boolean = false,
    val assetPriceHistory: DataResource<CoinviewAssetPriceHistory> = DataResource.Loading,
    val requestedTimeSpan: HistoricalTimeSpan? = null,
    val interactiveAssetPrice: CoinviewAssetPrice? = null,

    // watchlist
    val watchlist: DataResource<Boolean> = DataResource.Loading,

    // asset detail (accounts/non tradeable)
    val assetDetail: DataResource<CoinviewAssetDetail> = DataResource.Loading,

    // recurring buys
    val recurringBuys: DataResource<CoinviewRecurringBuys> = DataResource.Loading,

    // quick actions
    val quickActions: DataResource<CoinviewQuickActions> = DataResource.Loading,

    // asset info
    val assetInfo: DataResource<DetailedAssetInformation> = DataResource.Loading,

    // news
    val newsArticles: DataResource<List<NewsArticle>> = DataResource.Loading,

    // alerts
    val alert: CoinviewPillAlert = CoinviewPillAlert.None,

    // errors
    val error: CoinviewError = CoinviewError.None,

    // deeplinks
    val recurringBuyId: String? = null
) : ModelState {
    val isTradeableAsset: Boolean?
        get() = (assetDetail as? DataResource.Data)?.data?.let { it is CoinviewAssetDetail.Tradeable }

    val accounts: CoinviewAccounts?
        get() = ((assetDetail as? DataResource.Data)?.data as? CoinviewAssetDetail.Tradeable)?.accounts

    /**
     * Returns the first account that is:
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
    fun actionableAccount(isPositiveBalanceRequired: Boolean = true): CoinviewAccount {
        check(assetDetail is DataResource.Data) {
            "accounts not initialized"
        }
        check(assetDetail.data is CoinviewAssetDetail.Tradeable) {
            "asset is not tradeable"
        }

        return with((assetDetail.data as CoinviewAssetDetail.Tradeable).accounts) {
            accounts.firstOrNull { account ->
                val isTradingAccount = account is CoinviewAccount.Custodial.Trading
                val isPrivateKeyAccount = account is CoinviewAccount.PrivateKey

                val isValidBalance = if (isPositiveBalanceRequired) {
                    account.cryptoBalance.map { it.isPositive }.dataOrElse(false)
                } else {
                    true
                }

                (isTradingAccount || isPrivateKeyAccount) && isValidBalance
            } ?: error(
                "No actionable account found - maybe a quick action is active when it should be disabled -- " +
                    "or you're expecting a zero balance account if so make sure isPositiveBalanceRequired = false "
            )
        }
    }
}

sealed interface CoinviewPillAlert {
    object WatchlistAdded : CoinviewPillAlert
    object None : CoinviewPillAlert
}

sealed interface CoinviewError {
    /**
     * Error that could occur when loading accounts fails
     */
    object AccountsLoadError : CoinviewError

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
