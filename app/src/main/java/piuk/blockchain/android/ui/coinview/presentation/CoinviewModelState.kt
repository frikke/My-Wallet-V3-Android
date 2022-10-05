package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
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
    val walletMode: WalletMode,

    val asset: CryptoAsset? = null,

    // price
    val isChartDataLoading: Boolean = false,
    val assetPriceHistory: DataResource<CoinviewAssetPriceHistory> = DataResource.Loading,
    val requestedTimeSpan: HistoricalTimeSpan? = null,
    val interactiveAssetPrice: CoinviewAssetPrice? = null,

    // asset detail (accounts/non tradeable)
    val assetDetail: DataResource<CoinviewAssetDetail> = DataResource.Loading,

    // recurring buys
    val recurringBuys: DataResource<CoinviewRecurringBuys> = DataResource.Loading,

    // quick actions
    val quickActions: DataResource<CoinviewQuickActions> = DataResource.Loading,

    // asset info
    val assetInfo: DataResource<DetailedAssetInformation> = DataResource.Loading,

    // errors
    val error: CoinviewError = CoinviewError.None
) : ModelState {
    val accounts: CoinviewAccounts?
        get() = ((assetDetail as? DataResource.Data)?.data as? CoinviewAssetDetail.Tradeable)?.accounts

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
            check(assetDetail is DataResource.Data) {
                "accounts not initialized"
            }
            check(assetDetail.data is CoinviewAssetDetail.Tradeable) {
                "asset is not tradeable"
            }

            return with((assetDetail.data as CoinviewAssetDetail.Tradeable).accounts) {
                accounts.firstOrNull { account ->
                    val isUniversalTradingDefiAccount = account is CoinviewAccount.Universal &&
                        (account.filter == AssetFilter.Trading || account.filter == AssetFilter.NonCustodial)
                    val isTradingAccount = account is CoinviewAccount.Custodial.Trading
                    val isDefiAccount = account is CoinviewAccount.Defi
                    val hasPositiveBalance = account.cryptoBalance.isPositive

                    (isUniversalTradingDefiAccount || isTradingAccount || isDefiAccount) && hasPositiveBalance
                } ?: error("No actionable account found - maybe a quick action is active when it should be disabled")
            }
        }
}

sealed interface CoinviewError {
    /**
     * Error that could occur when loading the account actions fails
     * @see GetAccountActionsUseCase
     */
    object ActionsLoadError : CoinviewError

    object None : CoinviewError
}
