package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.Money
import java.io.Serializable

sealed class DepositAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class DepositClicked(override val origin: LaunchOrigin) : DepositAnalytics(
        event = AnalyticsNames.DEPOSIT_CLICKED.eventName
    )

    class DepositAmountEntered(
        currency: String,
        amount: Money,
        paymentMethodType: PaymentMethodType
    ) : DepositAnalytics(
        event = AnalyticsNames.DEPOSIT_AMOUNT_ENTERED.eventName,
        params = mapOf(
            "currency" to currency,
            "deposit_method" to paymentMethodType.name,
            "amount" to amount.toBigDecimal()
        )
    )

    class DepositMethodSelected(
        currency: String,
        paymentMethodType: PaymentMethodType
    ) : DepositAnalytics(
        event = AnalyticsNames.DEPOSIT_METHOD_SELECTED.eventName,
        params = mapOf(
            "currency" to currency,
            "deposit_method" to paymentMethodType.name
        )
    )
}
