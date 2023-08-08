package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.charts.ChartEntry
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.image.LocalLogo
import com.blockchain.image.LogoValue
import com.blockchain.news.NewsArticle
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction

data class CoinviewViewState(
    val asset: DataResource<CoinviewAssetState>,
    val showKycRejected: Boolean,
    val assetPrice: DataResource<CoinviewPriceState>,
    val tradeable: CoinviewAssetTradeableState,
    val watchlist: DataResource<Boolean>,
    val accounts: DataResource<CoinviewAccountsState?>,
    val centerQuickAction: DataResource<List<CoinviewQuickAction>>,
    val recurringBuys: DataResource<CoinviewRecurringBuysState>,
    val bottomQuickAction: DataResource<List<CoinviewQuickAction>>,
    val assetInfo: CoinviewAssetInfoState,
    val news: CoinviewNewsState,
    val pillAlert: CoinviewPillAlertState,
    val snackbarError: CoinviewSnackbarAlertState
) : ViewState

// Asset
data class CoinviewAssetState(
    val asset: AssetInfo,
    val l1Network: CoinViewNetwork?
)

data class CoinViewNetwork(
    val logo: String,
    val name: String
)

// Price
data class CoinviewPriceState(
    val assetName: String,
    val assetLogo: String,
    val fiatSymbol: String,
    val price: String,
    val priceChange: String,
    val valueChange: ValueChange,
    val intervalText: PriceIntervalText,
    val chartData: CoinviewChartState,
    val selectedTimeSpan: HistoricalTimeSpan
) {
    sealed interface CoinviewChartState {
        object Loading : CoinviewChartState
        data class Data(val chartData: List<ChartEntry>) : CoinviewChartState
    }
}

// Tradeable
sealed interface CoinviewAssetTradeableState {
    object Tradeable : CoinviewAssetTradeableState
    data class NonTradeable(
        val assetName: String,
        val assetTicker: String
    ) : CoinviewAssetTradeableState
}

// Total balance
sealed interface CoinviewTotalBalanceState {
    object NotSupported : CoinviewTotalBalanceState
    object Loading : CoinviewTotalBalanceState
    object Error : CoinviewTotalBalanceState
    data class Data(
        val assetName: String,
        val totalFiatBalance: String,
        val totalCryptoBalance: String
    ) : CoinviewTotalBalanceState
}

// Accounts
data class CoinviewAccountsState(
    val assetName: String,
    val totalBalance: String,
    val accounts: List<CoinviewAccountState>
) {
    sealed interface CoinviewAccountState {
        // todo find a better way to identify an account for the viewmodel without sending the whole object
        val cvAccount: CoinviewAccount

        data class Available(
            override val cvAccount: CoinviewAccount,
            val title: String,
            val subtitle: TextValue?,
            val cryptoBalance: String,
            val fiatBalance: String,
            val logo: LogoValue,
            val assetColor: String
        ) : CoinviewAccountState

        data class Unavailable(
            override val cvAccount: CoinviewAccount,
            val title: String,
            val subtitle: TextValue,
            val logo: LogoValue
        ) : CoinviewAccountState
    }
}

// Recurring buys
sealed interface CoinviewRecurringBuysState {
    object Upsell : CoinviewRecurringBuysState
    data class Data(
        val recurringBuys: List<CoinviewRecurringBuyState>
    ) : CoinviewRecurringBuysState {
        data class CoinviewRecurringBuyState(
            val id: String,
            val description: TextValue,
            val status: TextValue,
            val assetColor: String
        )
    }
}

// Quick actions
fun CoinviewQuickAction.name(assetTicker: String) = when (this) {
    is CoinviewQuickAction.Buy -> com.blockchain.stringResources.R.string.common_buy
    is CoinviewQuickAction.Sell -> com.blockchain.stringResources.R.string.common_sell
    is CoinviewQuickAction.Send -> com.blockchain.stringResources.R.string.common_send
    is CoinviewQuickAction.Receive -> com.blockchain.stringResources.R.string.common_receive
    is CoinviewQuickAction.Swap -> com.blockchain.stringResources.R.string.common_swap
    is CoinviewQuickAction.Get -> com.blockchain.stringResources.R.string.common_get_asset
}.run {
    TextValue.IntResValue(this, args = listOf(assetTicker))
}

fun CoinviewQuickAction.logo() = when (this) {
    is CoinviewQuickAction.Buy -> LocalLogo.Buy
    is CoinviewQuickAction.Sell -> LocalLogo.Sell
    is CoinviewQuickAction.Send -> LocalLogo.Send
    is CoinviewQuickAction.Receive -> LocalLogo.Receive
    is CoinviewQuickAction.Swap,
    is CoinviewQuickAction.Get -> LocalLogo.Swap
}

// Info
sealed interface CoinviewAssetInfoState {
    object Loading : CoinviewAssetInfoState
    object Error : CoinviewAssetInfoState
    data class Data(
        val assetName: String,
        val description: String?,
        val website: String?
    ) : CoinviewAssetInfoState
}

// News
data class CoinviewNewsState(
    val newsArticles: List<NewsArticle>?
)

// Pill alerts
sealed interface CoinviewPillAlertState {
    val message: Int
    val icon: ImageResource.Local

    data class Alert(override val message: Int, override val icon: ImageResource.Local) : CoinviewPillAlertState {
        override fun equals(other: Any?): Boolean {
            return (other as? Alert)?.message == message
        }

        override fun hashCode(): Int = message
    }

    object None : CoinviewPillAlertState {
        override val message: Int get() = error("None error doesn't have message property")
        override val icon: ImageResource.Local get() = error("None error doesn't have icon property")
    }
}

// Snackbar errors
sealed interface CoinviewSnackbarAlertState {
    val message: Int
    val snackbarType: SnackbarType

    object AccountsLoadError : CoinviewSnackbarAlertState {
        override val message: Int = com.blockchain.stringResources.R.string.coinview_wallet_load_error
        override val snackbarType: SnackbarType = SnackbarType.Error
    }

    object ActionsLoadError : CoinviewSnackbarAlertState {
        override val message: Int = com.blockchain.stringResources.R.string.coinview_actions_error
        override val snackbarType: SnackbarType = SnackbarType.Warning
    }

    object WatchlistToggleError : CoinviewSnackbarAlertState {
        override val message: Int = com.blockchain.stringResources.R.string.coinview_watchlist_toggle_fail
        override val snackbarType: SnackbarType = SnackbarType.Warning
    }

    object None : CoinviewSnackbarAlertState {
        override val message: Int get() = error("None error doesn't have message property")
        override val snackbarType: SnackbarType get() = error("None error doesn't have snackbarType property")
    }
}

// misc
sealed interface ValueAvailability {
    data class Available(val value: String) : ValueAvailability
    object NotAvailable : ValueAvailability
}
