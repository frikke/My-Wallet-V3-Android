package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AssetAction
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
    val viewState: CoinViewViewState = CoinViewViewState.None,
    val assetDisplay: List<AssetDisplayInfo> = emptyList(),
    val error: CoinViewError = CoinViewError.None,
    val assetPrices: Prices24HrWithDelta? = null
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

    class ShowRecurringBuys(val recurringBuys: List<RecurringBuy>) : CoinViewViewState()
    class QuickActionsLoaded(
        val startAction: QuickActionCta,
        val endAction: QuickActionCta,
        val actionableAccount: BlockchainAccount
    ) : CoinViewViewState()
    object NonTradeableAccount : CoinViewViewState()
}

enum class QuickActionCta {
    Buy, Sell, Send, Receive
}

enum class CoinViewError {
    None,
    UnknownAsset,
    WalletLoadError,
    ChartLoadError,
    RecurringBuysLoadError,
    QuickActionsFailed
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

sealed class AssetDetailsItem {
    data class CryptoDetailsInfo(
        val assetFilter: AssetFilter,
        val account: BlockchainAccount,
        val balance: Money,
        val fiatBalance: Money,
        val actions: Set<AssetAction>,
        val interestRate: Double = Double.NaN
    ) : AssetDetailsItem()

    data class RecurringBuyInfo(
        val recurringBuy: RecurringBuy
    ) : AssetDetailsItem()

    object RecurringBuyBanner : AssetDetailsItem()
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

internal sealed class Details {
    object NoDetails : Details()
    class DetailsItem(
        val isEnabled: Boolean,
        val account: BlockchainAccount,
        val balance: Money,
        val pendingBalance: Money,
        val actions: Set<StateAwareAction>,
        val isDefault: Boolean = false
    ) : Details()
}
