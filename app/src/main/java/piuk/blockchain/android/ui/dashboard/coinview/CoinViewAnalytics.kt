package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import java.io.Serializable

sealed class CoinViewAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class ChartEngaged(
        override val origin: LaunchOrigin,
        currency: String,
        timeInterval: TimeInterval,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_CHART_ENGAGED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TIME_INTERVAL to timeInterval.interval,
            )
        )

    class ChartDisengaged(
        override val origin: LaunchOrigin,
        currency: String,
        timeInterval: TimeInterval,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_CHART_DISENGAGED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TIME_INTERVAL to timeInterval.interval,
            )
        )

    class ChartTimeIntervalSelected(
        override val origin: LaunchOrigin,
        currency: String,
        timeInterval: TimeInterval,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_CHART_INTERVAL_SELECTED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TIME_INTERVAL to timeInterval.interval,
            )
        )

    class CoinAddedFromWatchlist(
        override val origin: LaunchOrigin,
        currency: String
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_ADDED_WATCHLIST.eventName,
            params = mapOf(
                CURRENCY to currency
            )
        )

    class CoinRemovedFromWatchlist(
        override val origin: LaunchOrigin,
        currency: String
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_REMOVED_FROM_WATCHLIST.eventName,
            params = mapOf(
                CURRENCY to currency
            )
        )

    class CoinViewOpen(
        override val origin: LaunchOrigin,
        currency: String
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_COINVIEW_OPEN.eventName,
            params = mapOf(
                CURRENCY to currency
            )
        )

    class CoinViewClosed(
        closingMethod: ClosingMethod,
        currency: String
    ) : CoinViewAnalytics(
        event = AnalyticsNames.COINVIEW_COINVIEW_CLOSE.eventName,
        params = mapOf(
            CURRENCY to currency,
            CLOSING_METHOD to closingMethod.name
        )
    )

    // Cannot be used yet.
    class ConnectToTheExchangeActioned(
        override val origin: LaunchOrigin,
        currency: String
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_CONNECT_EXCHANGE_ACTIONED.eventName,
            params = mapOf(
                CURRENCY to currency
            )
        )

    class ExplainerAccepted(
        override val origin: LaunchOrigin,
        currency: String,
        accountType: AccountType
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_EXPLAINER_ACCEPTED.eventName,
            params = mapOf(
                CURRENCY to currency,
                ACCOUNT_TYPE to accountType.name,
            )
        )

    class ExplainerViewed(
        override val origin: LaunchOrigin,
        currency: String,
        accountType: AccountType
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_EXPLAINER_VIEWED.eventName,
            params = mapOf(
                CURRENCY to currency,
                ACCOUNT_TYPE to accountType.name,
            )
        )

    class HyperlinkClicked(
        override val origin: LaunchOrigin,
        currency: String,
        selection: Selection,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_HYPERLINK_CLICKED.eventName,
            params = mapOf(
                CURRENCY to currency,
                SELECTION to selection.name,
            )
        )

    class TransactionTypeClicked(
        override val origin: LaunchOrigin,
        currency: String,
        transactionType: TransactionType,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_TRANSACTION_CLICKED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TRANSACTION_TYPE to transactionType.name,
            )
        )

    class WalletsAccountsClicked(
        override val origin: LaunchOrigin,
        currency: String,
        accountType: AccountType,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_WALLETS_ACCOUNTS_CLICKED.eventName,
            params = mapOf(
                CURRENCY to currency,
                ACCOUNT_TYPE to accountType.name,
            )
        )

    class WalletsAccountsViewed(
        override val origin: LaunchOrigin,
        currency: String,
        accountType: AccountType,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_WALLETS_ACCOUNTS_VIEWED.eventName,
            params = mapOf(
                CURRENCY to currency,
                ACCOUNT_TYPE to accountType.name,
            )
        )

    object CoinViewBuyClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.COIN_VIEW_BUY_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object CoinViewAccountBuyClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.COIN_VIEW_ACCOUNT_BUY_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    class BuySellClicked(
        override val origin: LaunchOrigin,
        currency: String,
        type: Type
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.BUY_SELL_CLICKED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TYPE to type.name,
            )
        )

    class BuyReceiveClicked(
        override val origin: LaunchOrigin,
        currency: String,
        type: Type
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_BUY_RECEIVE_CLICKED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TYPE to type.name,
            )
        )

    class SendReceiveClicked(
        override val origin: LaunchOrigin,
        currency: String,
        type: Type,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_SEND_RECEIVE_CLICKED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TYPE to type.name,
            )
        )

    class RewardsWithdrawOrAddClicked(
        override val origin: LaunchOrigin,
        currency: String,
        type: Type,
    ) :
        CoinViewAnalytics(
            event = AnalyticsNames.COINVIEW_REWARDS_WITHDRAW_ADD_CLICKED.eventName,
            params = mapOf(
                CURRENCY to currency,
                TYPE to type.name,
            )
        )

    companion object {
        private const val CURRENCY = "currency"
        private const val ACCOUNT_TYPE = "account_type"
        private const val SELECTION = "selection"
        private const val TRANSACTION_TYPE = "transaction_type"
        private const val TIME_INTERVAL = "time_interval"
        private const val TYPE = "type"
        private const val CLOSING_METHOD = "closing_method"

        enum class Type {
            RECEIVE, SEND, ADD, WITHDRAW, SELL, BUY
        }

        enum class ClosingMethod {
            CLICKED_GREY_AREA, DRAGGED_DOWN, X_BUTTON,
            BACK_BUTTON
        }

        enum class Selection {
            EXPLORER, LEARN_MORE, OFFICIAL_WEBSITE_WEB, VIEW_LEGAL, WEBSITE_WALLET, WHITE_PAPER
        }

        enum class AccountType {
            CUSTODIAL, EXCHANGE_ACCOUNT, REWARDS_ACCOUNT, USERKEY
        }

        enum class TransactionType {
            ACTIVITY, ADD, BUY, DEPOSIT, RECEIVE, REWARDS_SUMMARY, SELL, SEND, SWAP, WITHDRAW
        }

        enum class TimeInterval(val interval: String) {
            DAY("1D"),
            WEEK("1W"),
            MONTH("1M"),
            YEAR("1Y"),
            ALL_TIME("ALL"),
            LIVE("LIVE"),
        }

        enum class Tier(val tierToUpgrade: String) {
            SILVER("1"),
            GOLD("2"),
            SILVER_PLUS("3"),
            PLATINUM("5")
        }
    }
}
