package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

sealed class CoinViewViewState {
    object None : CoinViewViewState()
    object LoadingWallets : CoinViewViewState()
    object LoadingChart : CoinViewViewState()
    object LoadingRecurringBuys : CoinViewViewState()
    object LoadingAssetDetails : CoinViewViewState()
    object LoadingQuickActions : CoinViewViewState()
    class ShowAccountInfo(
        val totalCryptoBalance: Map<AssetFilter, Money>,
        val totalFiatBalance: Money,
        val assetDetails: List<AssetDetailsItem.CryptoDetailsInfo>,
        val isAddedToWatchlist: Boolean
    ) : CoinViewViewState()

    class ShowAssetInfo(
        val entries: List<ChartEntry>,
        val prices: Prices24HrWithDelta,
        val historicalRateList: HistoricalRateList,
        val selectedFiat: FiatCurrency
    ) : CoinViewViewState()

    class ShowRecurringBuys(val recurringBuys: List<RecurringBuy>, val shouldShowUpsell: Boolean) : CoinViewViewState()
    class QuickActionsLoaded(
        val middleAction: QuickActionCta,
        val startAction: QuickActionCta,
        val endAction: QuickActionCta,
        val actionableAccount: BlockchainAccount
    ) : CoinViewViewState()

    class ShowAssetDetails(val details: DetailedAssetInformation) : CoinViewViewState()
    class ShowNonTradeableAccount(val isAddedToWatchlist: Boolean) : CoinViewViewState()
    class UpdatedWatchlist(val addedToWatchlist: Boolean) : CoinViewViewState()
    class ShowAccountActionSheet(val actions: Array<StateAwareAction>) : CoinViewViewState()
    class ShowAccountExplainerSheet(val actions: Array<StateAwareAction>) : CoinViewViewState()
    class ShowBalanceUpsellSheet(
        val account: BlockchainAccount,
        val action: AssetAction,
        val canBuy: Boolean
    ) : CoinViewViewState()
}

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

sealed interface AssetDetailsItem {
    /**
     * Model used for delegates
     */
    sealed class CryptoDetailsInfo(
        open val account: BlockchainAccount,
        open val balance: Money,
        open val fiatBalance: Money,
        open val actions: Set<StateAwareAction>,
        open val interestRate: Double,
        open val stakingRate: Double
    ) : AssetDetailsItem

    /**
     * used when wallet mode is Universal or Custodial
     */
    data class BrokerageDetailsInfo(
        val assetFilter: AssetFilter,
        override val account: BlockchainAccount,
        override val balance: Money,
        override val fiatBalance: Money,
        override val actions: Set<StateAwareAction>,
        override val interestRate: Double = Double.NaN,
        override val stakingRate: Double = Double.NaN
    ) : CryptoDetailsInfo(
        account = account,
        balance = balance,
        fiatBalance = fiatBalance,
        actions = actions,
        interestRate = interestRate,
        stakingRate = stakingRate
    )

    /**
     * used when wallet mode is NonCustodial
     */
    data class DefiDetailsInfo(
        override val account: BlockchainAccount,
        override val balance: Money,
        override val fiatBalance: Money,
        override val actions: Set<StateAwareAction>,
    ) : CryptoDetailsInfo(
        account = account,
        balance = balance,
        fiatBalance = fiatBalance,
        actions = actions,
        interestRate = Double.NaN,
        stakingRate = Double.NaN
    )

    data class CentralCta(
        val enabled: Boolean,
        val account: BlockchainAccount,
    ) : AssetDetailsItem

    data class RecurringBuyInfo(
        val recurringBuy: RecurringBuy
    ) : AssetDetailsItem

    object RecurringBuyBanner : AssetDetailsItem

    object RecurringBuyError : AssetDetailsItem

    object AccountError : AssetDetailsItem
}
