package com.blockchain.earn

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.earn.dashboard.viewmodel.EarnType
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

    object DiscoverClicked : EarnAnalytics(AnalyticsNames.SUPERAPP_EARN_DISCOVER_CLICKED.eventName)

    data class LearnMoreClicked(
        val product: EarnType
    ) : EarnAnalytics(
        event = AnalyticsNames.SUPERAPP_EARN_LEARN_MORE_CLICKED.eventName,
        params = mapOf(EARN_PRODUCT to product.typeName())
    )

    data class AddClicked(
        val currency: String,
        val product: EarnType
    ) : EarnAnalytics(
        event = AnalyticsNames.SUPERAPP_EARN_DETAIL_ADD_CLICKED.eventName,
        params = mapOf(CURRENCY to currency, EARN_PRODUCT to product.typeName())
    )

    data class WithdrawClicked(
        val currency: String,
        val product: EarnType
    ) : EarnAnalytics(
        event = AnalyticsNames.SUPERAPP_EARN_DETAIL_WITHDRAW_CLICKED.eventName,
        params = mapOf(CURRENCY to currency, EARN_PRODUCT to product.typeName())
    )

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

    class StakingWithdrawalClicked(
        currency: String,
        origin: LaunchOrigin
    ) : EarnAnalytics(
        event = AnalyticsNames.STAKING_WITHDRAWAL_CLICKED.eventName,
        mapOf(
            CURRENCY to currency
        ),
        origin = origin
    )

    class ActiveRewardsDepositClicked(
        currency: String,
        origin: LaunchOrigin
    ) : EarnAnalytics(
        event = AnalyticsNames.ACTIVE_REWARDS_DEPOSIT_CLICKED.eventName,
        mapOf(
            CURRENCY to currency
        ),
        origin = origin
    )

    class ActiveRewardsWithdrawalClicked(
        currency: String,
        origin: LaunchOrigin
    ) : EarnAnalytics(
        event = AnalyticsNames.ACTIVE_REWARDS_WITHDRAWAL_CLICKED.eventName,
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
        private const val EARN_PRODUCT = "earn_product"
    }
}

@Deprecated("use from common:presentation")
enum class TxFlowAnalyticsAccountType {
    TRADING, USERKEY, SAVINGS, EXTERNAL;

    companion object {
        fun fromAccount(account: BlockchainAccount): TxFlowAnalyticsAccountType =
            when (account) {
                is TradingAccount,
                is BankAccount -> TRADING
                is EarnRewardsAccount.Interest -> SAVINGS
                else -> USERKEY
            }

        fun fromTransactionTarget(transactionTarget: TransactionTarget): TxFlowAnalyticsAccountType {
            (transactionTarget as? BlockchainAccount)?.let {
                return fromAccount(it)
            } ?: return EXTERNAL
        }
    }
}

private fun EarnType.typeName() = when (this) {
    EarnType.Passive -> "SAVINGS"
    EarnType.Staking -> "STAKING"
    EarnType.Active -> "ACTIVE"
}
