package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.coincore.FeeLevel
import com.blockchain.earn.TxFlowAnalyticsAccountType
import com.blockchain.extensions.filterNotNullValues
import info.blockchain.balance.Currency
import java.io.Serializable
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.FEE_SCHEDULE
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.PARAM_ASSET
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.PARAM_ERROR
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.PARAM_NEW_FEE
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.PARAM_OLD_FEE
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.PARAM_SOURCE
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.PARAM_TARGET

sealed class SendAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object EnterAddressDisplayed : SendAnalyticsEvent("send_address_screen_seen")
    object QrCodeScanned : SendAnalyticsEvent("send_form_qr_button_click")
    object EnterAddressCtaClick : SendAnalyticsEvent("send_address_click_confirm")
    object EnterAmountDisplayed : SendAnalyticsEvent("send_enter_amount_seen")
    object SendMaxClicked : SendAnalyticsEvent("send_max_amount_clicked")
    object EnterAmountCtaClick : SendAnalyticsEvent("send_enter_amount_confirm")
    object ConfirmationsDisplayed : SendAnalyticsEvent("send_summary_shown")
    object CancelTransaction : SendAnalyticsEvent("send_summary_cancel")

    data class ConfirmTransaction(
        val asset: Currency,
        val source: String,
        val target: String,
        val feeLevel: String
    ) : SendAnalyticsEvent(
        "send_summary_confirm",
        mapOf(
            PARAM_ASSET to asset.networkTicker,
            PARAM_SOURCE to source,
            PARAM_TARGET to target,
            FEE_SCHEDULE to feeLevel
        )
    )

    data class TransactionSuccess(val asset: Currency, val target: String, val source: String) :
        SendAnalyticsEvent(
            "send_confirm_success",
            mapOf(
                PARAM_ASSET to asset.networkTicker,
                PARAM_TARGET to target,
                PARAM_SOURCE to source
            )
        )

    data class TransactionFailure(
        val asset: Currency,
        val target: String?,
        val source: String?,
        val error: String
    ) : SendAnalyticsEvent(
        "send_confirm_error",
        mapOf(
            PARAM_ASSET to asset.networkTicker,
            PARAM_TARGET to target,
            PARAM_SOURCE to source,
            PARAM_ERROR to error
        ).filterNotNullValues()
    )

    data class FeeChanged(val oldFee: FeeLevel, val newFee: FeeLevel) :
        SendAnalyticsEvent(
            "send_change_fee_click",
            mapOf(
                PARAM_OLD_FEE to oldFee.name,
                PARAM_NEW_FEE to newFee.name
            )
        )

    class SendAmountMaxClicked(
        private val currency: String,
        private val fromAccountType: TxFlowAnalyticsAccountType,
        private val toAccountType: TxFlowAnalyticsAccountType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SEND_MAX_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "currency" to currency,
                "from_account_type" to fromAccountType.name,
                "to_account_type" to toAccountType.name
            )
    }

    class SendSourceAccountSelected(
        private val currency: String,
        private val fromAccountType: TxFlowAnalyticsAccountType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SEND_FROM_SELECTED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "currency" to currency,
                "from_account_type" to fromAccountType.name
            )
    }

    class SendSubmitted(
        private val currency: String,
        private val feeType: AnalyticsFeeType,
        private val fromAccountType: TxFlowAnalyticsAccountType,
        private val toAccountType: TxFlowAnalyticsAccountType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SEND_SUBMITTED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "currency" to currency,
                "fee_rate" to feeType.name,
                "from_account_type" to fromAccountType.name,
                "to_account_type" to toAccountType.name
            )
    }
}
