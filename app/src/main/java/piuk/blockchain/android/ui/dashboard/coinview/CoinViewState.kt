package piuk.blockchain.android.ui.dashboard.coinview

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
    val selectedCryptoAccount: AssetDetailsItemNew.CryptoDetailsInfo? = null,
    val viewState: CoinViewViewState = CoinViewViewState.None,
    val assetDisplay: List<AssetDisplayInfo> = emptyList(),
    val error: CoinViewError = CoinViewError.None,
    val assetPrices: Prices24HrWithDelta? = null,
    val hasActionBuyWarning: Boolean = false
) : MviState

sealed class CoinViewViewState {
    object None : CoinViewViewState()
    object LoadingWallets : CoinViewViewState()
    object LoadingChart : CoinViewViewState()
    object LoadingRecurringBuys : CoinViewViewState()
    object LoadingQuickActions : CoinViewViewState()
    class ShowAccountInfo(val assetInfo: AssetInformation.AccountsInfo) : CoinViewViewState()
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

    object NonTradeableAccount : CoinViewViewState()
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
    ActionsLoadError
}

sealed class AssetInformation(
    open val prices: Prices24HrWithDelta,
) {
    data class AccountsInfo(
        override val prices: Prices24HrWithDelta,
        val accountsList: List<AssetDisplayInfo>,
        val totalCryptoBalance: Money,
        val totalFiatBalance: Money
    ) : AssetInformation(prices)

    class NonTradeable(
        override val prices: Prices24HrWithDelta,
    ) : AssetInformation(prices)
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

sealed class AssetDetailsItemNew {
    data class CryptoDetailsInfo(
        val assetFilter: AssetFilter,
        val account: BlockchainAccount,
        val balance: Money,
        val fiatBalance: Money,
        val actions: Set<StateAwareAction>,
        val interestRate: Double = Double.NaN
    ) : AssetDetailsItemNew()

    data class RecurringBuyInfo(
        val recurringBuy: RecurringBuy
    ) : AssetDetailsItemNew()

    object RecurringBuyBanner : AssetDetailsItemNew()

    object RecurringBuyError : AssetDetailsItemNew()

    object AccountError : AssetDetailsItemNew()
}

internal sealed class Details {
    class DetailsItem(
        val isEnabled: Boolean,
        val account: BlockchainAccount,
        val balance: Money,
        val pendingBalance: Money,
        val actions: Set<StateAwareAction>,
        val isDefault: Boolean = false
    ) : Details()
}
