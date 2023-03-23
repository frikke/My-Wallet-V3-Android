package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.earn.TxFlowAnalyticsAccountType
import com.blockchain.extensions.filterNotNullValues
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.Money
import java.io.Serializable
import piuk.blockchain.android.simplebuy.AmountType
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.constructMap

class SellAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    constructor(event: SellAnalytics, asset: Currency, source: String) : this(
        event.value,
        constructMap(
            asset = asset, source = source, target = WALLET_TYPE_CUSTODIAL
        )
    )
}

enum class SellAnalytics(internal val value: String) {
    ConfirmationsDisplayed("sell_checkout_shown"),
    ConfirmTransaction("send_summary_confirm"),
    CancelTransaction("sell_checkout_cancel"),
    TransactionFailed("sell_checkout_error"),
    TransactionSuccess("sell_checkout_success"),
    EnterAmountCtaClick("sell_amount_confirm_click")
}

class MaxAmountClicked(
    private val sourceAccountType: TxFlowAnalyticsAccountType,
    private val inputCurrency: String,
    private val outputCurrency: String
) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SELL_AMOUNT_MAX_CLICKED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            FROM_ACCOUNT_TYPE to sourceAccountType.name,
            INPUT_CURRENCY to inputCurrency,
            OUTPUT_CURRENCY to outputCurrency
        )
}

class SellSourceAccountSelected(
    private val sourceAccountType: TxFlowAnalyticsAccountType,
    private val inputCurrency: String
) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SELL_SOURCE_SELECTED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            FROM_ACCOUNT_TYPE to sourceAccountType.name,
            INPUT_CURRENCY to inputCurrency
        )
}

object SellAssetScreenViewedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_ASSET_SCREEN_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

class SellAssetSelectedEvent(
    type: String,
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_ASSET_SELECTED.eventName
    override val params: Map<String, String> = mapOf(
        "type" to type,
    )
}

class SellFiatCryptoSwitcherClickedEvent(private val newInput: Currency) : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_FIAT_CRYPTO_SWITCHER_CLICKED.eventName
    override val params: Map<String, String> = mapOf(
        "switch_to" to if (newInput.type == CurrencyType.FIAT) "FIAT" else "CRYPTO"
    )
}

object SellAmountScreenViewedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_AMOUNT_SCREEN_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

class SellQuickFillButtonClicked(
    amount: String,
    amountType: AmountType,
    currency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_QUICK_FILL_BUTTON_CLICKED.eventName
    override val params: Map<String, String> = mapOf(
        "action" to "SELL",
        "amount" to amount,
        "amount_type" to amountType.name,
        "currency" to currency
    )
}

class SellAmountScreenNextClicked(
    private val sourceAccountType: TxFlowAnalyticsAccountType,
    private val amount: Money,
    private val outputCurrency: String
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_AMOUNT_SCREEN_NEXT_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        FROM_ACCOUNT_TYPE to sourceAccountType.name,
        INPUT_AMOUNT to amount.toBigDecimal(),
        INPUT_CURRENCY to amount.currencyCode,
        OUTPUT_CURRENCY to outputCurrency
    ).filterNotNullValues()
}

object SellCheckoutScreenViewedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_CHECKOUT_SCREEN_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object SellPriceTooltipClickedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_PRICE_TOOLTIP_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object SellCheckoutNetworkFeesClickedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_CHECKOUT_NETWORK_FEES_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object SellCheckoutScreenSubmittedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_CHECKOUT_SCREEN_SUBMITTED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object SellCheckoutScreenBackClickedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_CHECKOUT_SCREEN_BACK_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object SellAmountScreenBackClickedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.SELL_AMOUNT_SCREEN_BACK_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object FabSellClickedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.FAB_SELL_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object CoinViewSellClickedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.COIN_VIEW_SELL_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object CoinViewAccountSellClickedEvent : AnalyticsEvent {
    override val event: String = AnalyticsNames.COIN_VIEW_ACCOUNT_SELL_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

private const val FROM_ACCOUNT_TYPE = "from_account_type"
private const val INPUT_CURRENCY = "input_currency"
private const val OUTPUT_CURRENCY = "output_currency"
private const val INPUT_AMOUNT = "input_amount"
