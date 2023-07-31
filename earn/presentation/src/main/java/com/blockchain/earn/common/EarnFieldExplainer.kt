package com.blockchain.earn.common

import androidx.annotation.StringRes
import com.blockchain.earn.R

sealed class EarnFieldExplainer(
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int
) {

    object None : EarnFieldExplainer(
        titleResId = 0,
        descriptionResId = 0
    )
    object InterestRate : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.rewards_summary_rate,
        descriptionResId = com.blockchain.stringResources.R.string.earn_interest_rate_description
    )

    object MonthlyAccruedInterest : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.earn_interest_accrued_this_month,
        descriptionResId = com.blockchain.stringResources.R.string.earn_interest_accrued_this_month_description
    )

    object MonthlyPaymentFrequency : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.earn_payment_frequency,
        descriptionResId = com.blockchain.stringResources.R.string.earn_payment_frequency_monthly_description
    )

    object HoldPeriod : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.earn_interest_hold_period,
        descriptionResId = com.blockchain.stringResources.R.string.earn_interest_hold_period_description
    )

    object StakingEarnRate : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.rewards_summary_rate,
        descriptionResId = com.blockchain.stringResources.R.string.earn_staking_rate_description
    )

    object ActiveRewardsEarnRate : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.rewards_summary_rate,
        descriptionResId = com.blockchain.stringResources.R.string.earn_active_rewards_rate_description
    )

    object ActiveRewardsEarnings : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.earn_active_rewards_earnings,
        descriptionResId = com.blockchain.stringResources.R.string.earn_active_rewards_earnings_description
    )

    object ActiveRewardsOnHold : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.earn_active_rewards_on_hold,
        descriptionResId = com.blockchain.stringResources.R.string.earn_active_rewards_on_hold_description
    )

    object ActiveRewardsTriggerPrice : EarnFieldExplainer(
        titleResId = com.blockchain.stringResources.R.string.earn_active_rewards_trigger_price,
        descriptionResId = com.blockchain.stringResources.R.string.earn_active_rewards_trigger_price_description
    )
}
