package piuk.blockchain.android.ui.coinview.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.charts.ChartEntry
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction

data class CoinviewViewState(
    val asset: CoinviewAssetState,
    val assetPrice: CoinviewPriceState,
    val tradeable: CoinviewAssetTradeableState,
    val watchlist: CoinviewWatchlistState,
    val accounts: DataResource<CoinviewAccountsState?>,
    val centerQuickAction: DataResource<List<CoinviewQuickActionState>>,
    val recurringBuys: CoinviewRecurringBuysState,
    val bottomQuickAction: DataResource<List<CoinviewQuickActionState>>,
    val assetInfo: CoinviewAssetInfoState,
    val snackbarError: CoinviewSnackbarAlertState
) : ViewState

// Asset
sealed interface CoinviewAssetState {
    object Error : CoinviewAssetState
    data class Data(
        val asset: AssetInfo
    ) : CoinviewAssetState
}

// Price
sealed interface CoinviewPriceState {
    object Loading : CoinviewPriceState
    object Error : CoinviewPriceState
    data class Data(
        val assetName: String,
        val assetLogo: String,
        val fiatSymbol: String,
        val price: String,
        val priceChange: String,
        val percentChange: Double,
        @StringRes val intervalName: Int,
        val chartData: CoinviewChartState,
        val selectedTimeSpan: HistoricalTimeSpan
    ) : CoinviewPriceState {
        sealed interface CoinviewChartState {
            object Loading : CoinviewChartState
            data class Data(val chartData: List<ChartEntry>) : CoinviewChartState
        }
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

// Watchlist
sealed interface CoinviewWatchlistState {
    object NotSupported : CoinviewWatchlistState
    object Loading : CoinviewWatchlistState
    object Error : CoinviewWatchlistState
    data class Data(
        val isInWatchlist: Boolean
    ) : CoinviewWatchlistState
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
    val totalBalance: String,
    val accounts: List<CoinviewAccountState>
) {
    sealed interface CoinviewAccountState {
        // todo find a better way to identify an account for the viewmodel without sending the whole object
        val cvAccount: CoinviewAccount

        data class Available(
            override val cvAccount: CoinviewAccount,
            val title: String,
            val subtitle: TextValue,
            val cryptoBalance: String,
            val fiatBalance: String,
            val logo: LogoSource,
            val assetColor: String,
        ) : CoinviewAccountState

        data class Unavailable(
            override val cvAccount: CoinviewAccount,
            val title: String,
            val subtitle: TextValue,
            val logo: LogoSource
        ) : CoinviewAccountState
    }
}

// Recurring buys
sealed interface CoinviewRecurringBuysState {
    object NotSupported : CoinviewRecurringBuysState
    object Loading : CoinviewRecurringBuysState
    object Error : CoinviewRecurringBuysState
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
sealed interface CoinviewQuickActionState {
    val name: TextValue
    val logo: LogoSource.Resource

    object Buy : CoinviewQuickActionState {
        override val name = TextValue.IntResValue(R.string.common_buy)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_buy)
    }

    object Sell : CoinviewQuickActionState {
        override val name = TextValue.IntResValue(R.string.common_sell)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_sell)
    }

    object Send : CoinviewQuickActionState {
        override val name = TextValue.IntResValue(R.string.common_send)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_send)
    }

    object Receive : CoinviewQuickActionState {
        override val name = TextValue.IntResValue(R.string.common_receive)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_receive)
    }

    object Swap : CoinviewQuickActionState {
        override val name = TextValue.IntResValue(R.string.common_swap)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_swap)
    }
}

fun CoinviewQuickAction.toViewState(): CoinviewQuickActionState = run {
    when (this) {
        is CoinviewQuickAction.Buy -> CoinviewQuickActionState.Buy
        is CoinviewQuickAction.Sell -> CoinviewQuickActionState.Sell
        is CoinviewQuickAction.Send -> CoinviewQuickActionState.Send
        is CoinviewQuickAction.Receive -> CoinviewQuickActionState.Receive
        is CoinviewQuickAction.Swap -> CoinviewQuickActionState.Swap
    }
}

fun CoinviewQuickActionState.toModelState(): CoinviewQuickAction = run {
    when (this) {
        is CoinviewQuickActionState.Buy -> CoinviewQuickAction.Buy
        is CoinviewQuickActionState.Sell -> CoinviewQuickAction.Sell
        is CoinviewQuickActionState.Send -> CoinviewQuickAction.Send
        is CoinviewQuickActionState.Receive -> CoinviewQuickAction.Receive
        is CoinviewQuickActionState.Swap -> CoinviewQuickAction.Swap
    }
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

// Snackbar errors
sealed interface CoinviewSnackbarAlertState {
    val message: Int
    val snackbarType: SnackbarType

    object AccountsLoadError : CoinviewSnackbarAlertState {
        override val message: Int = R.string.coinview_wallet_load_error
        override val snackbarType: SnackbarType = SnackbarType.Error
    }

    object ActionsLoadError : CoinviewSnackbarAlertState {
        override val message: Int = R.string.coinview_actions_error
        override val snackbarType: SnackbarType = SnackbarType.Warning
    }

    object WatchlistToggleError : CoinviewSnackbarAlertState {
        override val message: Int = R.string.coinview_watchlist_toggle_fail
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

/**
 * Logo can either be Remote with a String URL - or Local with a drawable resource
 */
sealed interface LogoSource {
    data class Remote(val value: String) : LogoSource
    data class Resource(@DrawableRes val value: Int) : LogoSource
}
