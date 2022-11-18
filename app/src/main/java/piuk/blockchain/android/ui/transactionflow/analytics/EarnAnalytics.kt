package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import info.blockchain.balance.Money
import java.io.Serializable

sealed class EarnAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap(),
    override val origin: LaunchOrigin? = null
) : AnalyticsEvent {
    object InterestAnnouncementCta : EarnAnalytics("earn_banner_clicked")
    object InterestDashboardKyc : EarnAnalytics("earn_verify_identity_clicked")
    object InterestDashboardAction : EarnAnalytics("earn_interest_clicked")
    object InterestSummaryDepositCta : EarnAnalytics("earn_deposit_clicked")
    object InterestSummaryWithdrawCta : EarnAnalytics("earn_withdraw_clicked")

    class InterestClicked(override val origin: LaunchOrigin) :
        EarnAnalytics(AnalyticsNames.INTEREST_CLICKED.eventName)

    class InterestDepositAmountEntered(
        currency: String,
        sourceAccountType: TxFlowAnalyticsAccountType,
        inputAmount: Money
    ) : EarnAnalytics(
        event = AnalyticsNames.INTEREST_DEPOSIT_AMOUNT_ENTERED.eventName,
        mapOf(
            CURRENCY to currency,
            SOURCE_ACCOUNT_TYPE to sourceAccountType.name,
            INPUT_AMOUNT to inputAmount.toBigDecimal()
        )
    )

    class InterestDepositClicked(
        currency: String,
        origin: LaunchOrigin
    ) : EarnAnalytics(
        event = AnalyticsNames.INTEREST_DEPOSIT_CLICKED.eventName,
        mapOf(
            CURRENCY to currency
        ),
        origin = origin
    )

    class InterestDepositMaxAmount(
        currency: String,
        sourceAccountType: TxFlowAnalyticsAccountType
    ) : EarnAnalytics(
        event = AnalyticsNames.INTEREST_MAX_CLICKED.eventName,
        mapOf(
            CURRENCY to currency,
            SOURCE_ACCOUNT_TYPE to sourceAccountType.name
        )
    )

    object InterestDepositViewed : EarnAnalytics(
        event = AnalyticsNames.INTEREST_DEPOSIT_VIEWED.eventName
    )

    object InterestViewed : EarnAnalytics(
        event = AnalyticsNames.INTEREST_VIEWED.eventName
    )

    class InterestWithdrawalClicked(
        currency: String,
        origin: LaunchOrigin
    ) : EarnAnalytics(
        event = AnalyticsNames.INTEREST_WITHDRAWAL_CLICKED.eventName,
        mapOf(
            CURRENCY to currency
        ),
        origin = origin
    )

    object InterestWithdrawalViewed : EarnAnalytics(
        event = AnalyticsNames.INTEREST_WITHDRAWAL_VIEWED.eventName
    )

    class StakingDepositClicked(
        currency: String,
        origin: LaunchOrigin
    ) : EarnAnalytics(
        event = AnalyticsNames.STAKING_DEPOSIT_CLICKED.eventName,
        mapOf(
            CURRENCY to currency
        ),
        origin = origin
    )

    companion object {
        private const val CURRENCY = "currency"
        private const val SOURCE_ACCOUNT_TYPE = "from_account_type"
        private const val INPUT_AMOUNT = "input_amount"
        private const val INTEREST_RATE = "interest_rate"
    }
}
