package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.analytics.AnalyticsEvent
import info.blockchain.balance.Currency

sealed class InterestDepositAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {
    object EnterAmountSeen : InterestDepositAnalyticsEvent("earn_amount_screen_seen")
    object ConfirmationsSeen : InterestDepositAnalyticsEvent("earn_checkout_shown")
    object CancelTransaction : InterestDepositAnalyticsEvent("earn_checkout_cancel")

    data class ConfirmationsCtaClick(
        val asset: Currency
    ) : InterestDepositAnalyticsEvent(
        "earn_deposit_confirm_click",
        params = mapOf("asset" to asset.networkTicker)
    )

    data class EnterAmountCtaClick(
        val asset: Currency
    ) : InterestDepositAnalyticsEvent(
        "earn_amount_screen_confirm",
        params = mapOf("asset" to asset.networkTicker)
    )

    data class TransactionSuccess(
        val asset: Currency
    ) : InterestDepositAnalyticsEvent(
        "earn_checkout_success",
        params = mapOf("asset" to asset.networkTicker)
    )

    data class TransactionFailed(
        val asset: Currency
    ) : InterestDepositAnalyticsEvent(
        "earn_checkout_error",
        params = mapOf("asset" to asset.networkTicker)
    )
}
