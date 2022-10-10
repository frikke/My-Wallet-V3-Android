package piuk.blockchain.android.ui.coinview.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.charts.ChartEntry
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.core.price.HistoricalTimeSpan
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction

data class CoinviewViewState(
    val asset: CoinviewAssetState,
    val assetPrice: CoinviewPriceState,
    val tradeable: CoinviewAssetTradeableState,
    val watchlist: CoinviewWatchlistState,
    val totalBalance: CoinviewTotalBalanceState,
    val accounts: CoinviewAccountsState,
    val centerQuickAction: CoinviewCenterQuickActionsState,
    val recurringBuys: CoinviewRecurringBuysState,
    val bottomQuickAction: CoinviewBottomQuickActionsState,
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
sealed interface CoinviewAccountsState {
    object NotSupported : CoinviewAccountsState
    object Loading : CoinviewAccountsState
    object Error : CoinviewAccountsState
    data class Data(
        val style: CoinviewAccountsStyle,
        val header: CoinviewAccountsHeaderState,
        val accounts: List<CoinviewAccountState>
    ) : CoinviewAccountsState {
        sealed interface CoinviewAccountState {
            // todo find a better way to identify an account for the viewmodel without sending the whole object
            val cvAccount: CoinviewAccount

            data class Available(
                override val cvAccount: CoinviewAccount,
                val title: String,
                val subtitle: SimpleValue,
                val cryptoBalance: String,
                val fiatBalance: String,
                val logo: LogoSource,
                val assetColor: String,
            ) : CoinviewAccountState

            data class Unavailable(
                override val cvAccount: CoinviewAccount,
                val title: String,
                val subtitle: SimpleValue,
                val logo: LogoSource
            ) : CoinviewAccountState
        }

        sealed interface CoinviewAccountsHeaderState {
            data class ShowHeader(val text: SimpleValue) : CoinviewAccountsHeaderState
            object NoHeader : CoinviewAccountsHeaderState
        }
    }
}

/**
 * The accounts section can be drawn either boxed (defi) or simple (custodial)
 */
enum class CoinviewAccountsStyle {
    Simple, Boxed
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
            val description: SimpleValue,
            val status: SimpleValue,
            val assetColor: String
        )
    }
}

// Quick actions
// center
sealed interface CoinviewCenterQuickActionsState {
    object NotSupported : CoinviewCenterQuickActionsState
    object Loading : CoinviewCenterQuickActionsState
    data class Data(
        val center: CoinviewQuickActionState,
    ) : CoinviewCenterQuickActionsState
}

// bottom
sealed interface CoinviewBottomQuickActionsState {
    object NotSupported : CoinviewBottomQuickActionsState
    object Loading : CoinviewBottomQuickActionsState
    data class Data(
        val start: CoinviewQuickActionState,
        val end: CoinviewQuickActionState
    ) : CoinviewBottomQuickActionsState
}

sealed interface CoinviewQuickActionState {
    val name: SimpleValue
    val logo: LogoSource.Resource
    val enabled: Boolean

    data class Buy(override val enabled: Boolean) : CoinviewQuickActionState {
        override val name = SimpleValue.IntResValue(R.string.common_buy)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_buy)
    }

    data class Sell(override val enabled: Boolean) : CoinviewQuickActionState {
        override val name = SimpleValue.IntResValue(R.string.common_sell)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_sell)
    }

    data class Send(override val enabled: Boolean) : CoinviewQuickActionState {
        override val name = SimpleValue.IntResValue(R.string.common_send)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_send)
    }

    data class Receive(override val enabled: Boolean) : CoinviewQuickActionState {
        override val name = SimpleValue.IntResValue(R.string.common_receive)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_receive)
    }

    data class Swap(override val enabled: Boolean) : CoinviewQuickActionState {
        override val name = SimpleValue.IntResValue(R.string.common_swap)
        override val logo = LogoSource.Resource(R.drawable.ic_cta_swap)
    }

    object None : CoinviewQuickActionState {
        override val name: SimpleValue get() = error("None action doesn't have name property")
        override val logo: LogoSource.Resource get() = error("None action doesn't have log property")
        override val enabled: Boolean get() = error("None action doesn't have enabled property")
    }
}

fun CoinviewQuickAction.toViewState(): CoinviewQuickActionState = run {
    when (this) {
        is CoinviewQuickAction.Buy -> CoinviewQuickActionState.Buy(enabled)
        is CoinviewQuickAction.Sell -> CoinviewQuickActionState.Sell(enabled)
        is CoinviewQuickAction.Send -> CoinviewQuickActionState.Send(enabled)
        is CoinviewQuickAction.Receive -> CoinviewQuickActionState.Receive(enabled)
        is CoinviewQuickAction.Swap -> CoinviewQuickActionState.Swap(enabled)
        CoinviewQuickAction.None -> CoinviewQuickActionState.None
    }
}

fun CoinviewQuickActionState.toModelState(): CoinviewQuickAction = run {
    when (this) {
        is CoinviewQuickActionState.Buy -> CoinviewQuickAction.Buy(enabled)
        is CoinviewQuickActionState.Sell -> CoinviewQuickAction.Sell(enabled)
        is CoinviewQuickActionState.Send -> CoinviewQuickAction.Send(enabled)
        is CoinviewQuickActionState.Receive -> CoinviewQuickAction.Receive(enabled)
        is CoinviewQuickActionState.Swap -> CoinviewQuickAction.Swap(enabled)
        CoinviewQuickActionState.None -> CoinviewQuickAction.None
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
 * View text can either come as string or resource with args
 */
sealed interface SimpleValue {
    data class StringValue(val value: String) : SimpleValue
    data class IntResValue(
        @StringRes val value: Int,
        val args: List<Any> = emptyList()
    ) : SimpleValue
}

/**
 * Logo can either be Remote with a String URL - or Local with a drawable resource
 */
sealed interface LogoSource {
    data class Remote(val value: String) : LogoSource
    data class Resource(@DrawableRes val value: Int) : LogoSource
}
