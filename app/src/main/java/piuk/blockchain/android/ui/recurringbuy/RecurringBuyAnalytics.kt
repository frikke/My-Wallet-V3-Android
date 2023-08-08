package piuk.blockchain.android.ui.recurringbuy

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import java.io.Serializable

sealed class RecurringBuyAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class RecurringBuyCancelClicked(
        override val origin: LaunchOrigin,
        frequency: RecurringBuyFrequency,
        inputValue: Money,
        outputCurrency: AssetInfo,
        paymentMethodType: PaymentMethodType
    ) :
        RecurringBuyAnalytics(
            event = AnalyticsNames.RECURRING_BUY_CANCEL_CLICKED.eventName,
            params = mapOf(
                FREQUENCY to frequency.name,
                INPUT_AMOUNT to inputValue.toBigDecimal(),
                INPUT_CURRENCY to inputValue.currencyCode,
                OUTPUT_CURRENCY to outputCurrency.networkTicker,
                PAYMENT_METHOD to paymentMethodType.name
            )
        )

    class RecurringBuyClicked(
        override val origin: LaunchOrigin
    ) :
        RecurringBuyAnalytics(
            event = AnalyticsNames.RECURRING_BUY_CLICKED.eventName
        )

    class RecurringBuySuggestionSkipped(
        override val origin: LaunchOrigin
    ) : RecurringBuyAnalytics(
        event = AnalyticsNames.RECURRING_BUY_SUGGESTION_SKIPPED.eventName
    )

    class RecurringBuyDetailsClicked(
        override val origin: LaunchOrigin,
        cryptoCurrency: String
    ) : RecurringBuyAnalytics(
        event = AnalyticsNames.RECURRING_BUY_DETAILS_CLICKED.eventName,
        params = mapOf(CURRENCY to cryptoCurrency)
    )

    class RecurringBuyInfoViewed(
        page: Int
    ) : RecurringBuyAnalytics(
        event = AnalyticsNames.RECURRING_BUY_INFO_VIEWED.eventName,
        params = mapOf(PAGE to page)
    )

    class RecurringBuyLearnMoreClicked(
        override val origin: LaunchOrigin
    ) : RecurringBuyAnalytics(
        event = AnalyticsNames.RECURRING_BUY_LEARN_MORE_CLICKED.eventName
    )

    class RecurringBuyLearnMoreXSellClicked(
        override val origin: LaunchOrigin? = null,
        val dcaTitle: String
    ) : RecurringBuyAnalytics(
        event = AnalyticsNames.SUPERAPP_DEFI_DCA_LEARN_MORE_CLICKED.eventName,
        params = mapOf(MODULE_TITLE to dcaTitle)
    )

    object RecurringBuyViewed : RecurringBuyAnalytics(
        event = AnalyticsNames.RECURRING_BUY_VIEWED.eventName
    )

    class RecurringBuyUnavailableShown(
        reason: String
    ) : RecurringBuyAnalytics(
        event = AnalyticsNames.RECURRING_BUY_UNAVAILABLE_SHOWN.eventName,
        params = mapOf(REASON to reason)
    )

    companion object {
        private const val FREQUENCY = "frequency"
        private const val PAGE = "page"
        private const val REASON = "reason"
        private const val CURRENCY = "currency"
        private const val INPUT_AMOUNT = "input_amount"
        private const val INPUT_CURRENCY = "input_currency"
        private const val OUTPUT_CURRENCY = "output_currency"
        private const val PAYMENT_METHOD = "payment_method"
        const val SELECT_PAYMENT = "SELECT_PAYMENT"
        const val PAYMENT_METHOD_UNAVAILABLE = "PAYMENT_METHOD_UNAVAILABLE"
        private const val MODULE_TITLE = "module_title"
    }
}
