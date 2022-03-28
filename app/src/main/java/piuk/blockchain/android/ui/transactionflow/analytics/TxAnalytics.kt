package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.coincore.AssetAction
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import java.io.Serializable
import timber.log.Timber

class AmountSwitched(private val action: AssetAction, private val newInput: Currency) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.AMOUNT_SWITCHED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            "product" to action.toAnalyticsProduct(),
            "switch_to" to if (newInput.type == CurrencyType.FIAT) "FIAT" else "CRYPTO"
        )
}

class InfoBottomSheetKycUpsellActionClicked(
    action: AssetAction
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.TX_INFO_KYC_UPSELL_CLICKED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "flow_step" to action.toAnalyticsProduct()
    )
}

class InfoBottomSheetDismissed(
    action: AssetAction
) : AnalyticsEvent {
    override val event: String = AnalyticsNames.TX_INFO_KYC_UPSELL_DISMISSED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "flow_step" to action.toAnalyticsProduct()
    )
}

private fun AssetAction.toAnalyticsProduct(): String =
    when (this) {
        AssetAction.InterestDeposit,
        AssetAction.InterestWithdraw -> "SAVINGS"
        AssetAction.Buy -> "BUY"
        AssetAction.Sell -> "SELL"
        AssetAction.Send -> "SEND"
        AssetAction.Swap -> "SWAP"
        else -> {
            Timber.e(java.lang.IllegalArgumentException("Product not supported"))
            ""
        }
    }
