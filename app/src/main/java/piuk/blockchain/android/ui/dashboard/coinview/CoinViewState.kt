package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.nabu.models.data.RecurringBuy
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

data class CoinViewState(
    val asset: CryptoAsset? = null,
    val selectedFiat: FiatCurrency? = null,
    val selectedCryptoAccount: AssetDetailsItem.CryptoDetailsInfo? = null,
    val viewState: CoinViewViewState = CoinViewViewState.None,
    val assetDisplay: List<AssetDisplayInfo> = emptyList(),
    val error: CoinViewError = CoinViewError.None,
    val assetPrices: Prices24HrWithDelta? = null,
    val isAddedToWatchlist: Boolean = false,
    val hasActionBuyWarning: Boolean = false
) : MviState

sealed class CoinViewViewState {
    object None : CoinViewViewState()
    object LoadingWallets : CoinViewViewState()
    object LoadingChart : CoinViewViewState()
    object LoadingRecurringBuys : CoinViewViewState()
    object LoadingAssetDetails : CoinViewViewState()
    object LoadingQuickActions : CoinViewViewState()
    class ShowAccountInfo(val assetInfo: AssetInformation.AccountsInfo, val isAddedToWatchlist: Boolean) :
        CoinViewViewState()

    class ShowAssetInfo(
        val entries: List<ChartEntry>,
        val prices: Prices24HrWithDelta,
        val historicalRateList: HistoricalRateList,
        val selectedFiat: FiatCurrency
    ) : CoinViewViewState()

    class ShowRecurringBuys(val recurringBuys: List<RecurringBuy>, val shouldShowUpsell: Boolean) : CoinViewViewState()
    class QuickActionsLoaded(
        val startAction: QuickActionCta,
        val endAction: QuickActionCta,
        val actionableAccount: BlockchainAccount
    ) : CoinViewViewState()

    class ShowAssetDetails(val details: DetailedAssetInformation) : CoinViewViewState()
    class ShowNonTradeableAccount(val isAddedToWatchlist: Boolean) : CoinViewViewState()
    class UpdatedWatchlist(val addedToWatchlist: Boolean) : CoinViewViewState()
    class ShowAccountActionSheet(val actions: Array<StateAwareAction>) : CoinViewViewState()
    class ShowAccountExplainerSheet(val actions: Array<StateAwareAction>) : CoinViewViewState()
}

enum class QuickActionCta {
    Buy, Sell, Send, Receive, None
}

data class QuickActionData(
    val startAction: QuickActionCta,
    val endAction: QuickActionCta,
    val actionableAccount: BlockchainAccount
)

enum class CoinViewError {
    None,
    UnknownAsset,
    WalletLoadError,
    ChartLoadError,
    RecurringBuysLoadError,
    QuickActionsFailed,
    MissingSelectedFiat,
    MissingAssetPrices,
    WatchlistUpdateFailed,
    ActionsLoadError,
    AssetDetailsLoadError
}

sealed class AssetInformation(
    open val prices: Prices24HrWithDelta,
    open val isAddedToWatchlist: Boolean
) {
    data class AccountsInfo(
        override val isAddedToWatchlist: Boolean,
        override val prices: Prices24HrWithDelta,
        val accountsList: List<AssetDisplayInfo>,
        val totalCryptoBalance: Map<AssetFilter, Money>,
        val totalFiatBalance: Money
    ) : AssetInformation(prices, isAddedToWatchlist)

    class NonTradeable(
        override val isAddedToWatchlist: Boolean,
        override val prices: Prices24HrWithDelta,
    ) : AssetInformation(prices, isAddedToWatchlist)
}

data class AssetDisplayInfo(
    val account: BlockchainAccount,
    val filter: AssetFilter,
    val amount: Money,
    val pendingAmount: Money,
    val fiatValue: Money,
    val actions: Set<StateAwareAction>,
    val interestRate: Double = Double.NaN
)

sealed class AssetDetailsItem {
    data class CryptoDetailsInfo(
        val assetFilter: AssetFilter,
        val account: BlockchainAccount,
        val balance: Money,
        val fiatBalance: Money,
        val actions: Set<StateAwareAction>,
        val interestRate: Double = Double.NaN
    ) : AssetDetailsItem()

    data class RecurringBuyInfo(
        val recurringBuy: RecurringBuy
    ) : AssetDetailsItem()

    object RecurringBuyBanner : AssetDetailsItem()

    object RecurringBuyError : AssetDetailsItem()

    object AccountError : AssetDetailsItem()
}

class DetailsItem(
    val account: BlockchainAccount,
    val balance: Money,
    val pendingBalance: Money,
    val actions: Set<StateAwareAction>,
    val isDefault: Boolean = false
)
