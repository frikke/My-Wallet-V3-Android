package piuk.blockchain.android.ui.coinview.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.charts.ChartEntry
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.price.HistoricalTimeSpan
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction

data class CoinviewViewState(
    val assetName: String,
    val assetPrice: CoinviewPriceState,
    val totalBalance: CoinviewTotalBalanceState,
    val accounts: CoinviewAccountsState,
    val centerQuickAction: CoinviewCenterQuickActionsState,
    val recurringBuys: CoinviewRecurringBuysState,
    val bottomQuickAction: CoinviewBottomQuickActionsState
) : ViewState

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

// Total balance
sealed interface CoinviewTotalBalanceState {
    object NotSupported : CoinviewTotalBalanceState
    object Loading : CoinviewTotalBalanceState
    data class Data(
        val assetName: String,
        val totalFiatBalance: String,
        val totalCryptoBalance: String
    ) : CoinviewTotalBalanceState
}

// Accounts
sealed interface CoinviewAccountsState {
    object Loading : CoinviewAccountsState
    data class Data(
        val style: CoinviewAccountsStyle,
        val header: CoinviewAccountsHeaderState,
        val accounts: List<CoinviewAccountState>
    ) : CoinviewAccountsState {
        sealed interface CoinviewAccountState {
            data class Available(
                val title: String,
                val subtitle: SimpleValue,
                val cryptoBalance: String,
                val fiatBalance: String,
                val logo: LogoSource,
                val assetColor: String,
            ) : CoinviewAccountState

            data class Unavailable(
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
    object Loading : CoinviewCenterQuickActionsState
    data class Data(
        val center: CoinviewQuickActionState,
    ) : CoinviewCenterQuickActionsState
}

// bottom
sealed interface CoinviewBottomQuickActionsState {
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
        override val name = error("None action doesn't have name property")
        override val logo = error("None action doesn't have log property")
        override val enabled = error("None action doesn't have enabled property")
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

// misc
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
