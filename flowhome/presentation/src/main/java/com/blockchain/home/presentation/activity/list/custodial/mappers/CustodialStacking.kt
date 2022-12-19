package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.earn.domain.models.staking.StakingState
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate
import info.blockchain.wallet.multiaddress.TransactionSummary

@DrawableRes internal fun CustodialStakingActivitySummaryItem.iconSummary(): Int {
    return when (status) {
        StakingState.COMPLETE -> when (type) {
            TransactionSummary.TransactionType.DEPOSIT -> R.drawable.ic_activity_buy
            TransactionSummary.TransactionType.INTEREST_EARNED -> R.drawable.ic_activity_rewards
            TransactionSummary.TransactionType.WITHDRAW -> R.drawable.ic_activity_sell
            else -> R.drawable.ic_activity_buy
        }
        else -> R.drawable.ic_activity_rewards
    }
}

internal fun CustodialStakingActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                TransactionSummary.TransactionType.DEPOSIT -> R.string.tx_title_staked
                TransactionSummary.TransactionType.WITHDRAW -> R.string.tx_title_stake_withdrawn
                TransactionSummary.TransactionType.INTEREST_EARNED -> R.string.tx_title_stake_earned
                else -> R.string.tx_title_transferred
            },
            args = listOf(account.currency.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialStakingActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColor = when (status) {
        StakingState.REJECTED,
        StakingState.REFUNDED -> ActivityTextColor.Warning
        StakingState.FAILED -> ActivityTextColor.Error
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = when (status) {
            StakingState.COMPLETE -> TextValue.StringValue(date.toFormattedDate())
            StakingState.PENDING,
            StakingState.PROCESSING,
            StakingState.MANUAL_REVIEW -> TextValue.IntResValue(R.string.activity_state_pending)
            StakingState.FAILED -> TextValue.IntResValue(R.string.activity_state_failed)
            StakingState.CLEARED -> TextValue.IntResValue(R.string.activity_state_cleared)
            StakingState.REFUNDED -> TextValue.IntResValue(R.string.activity_state_refunded)
            StakingState.REJECTED -> TextValue.IntResValue(R.string.activity_state_rejected)
            StakingState.UNKNOWN -> TextValue.IntResValue(R.string.activity_state_unknown)
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun CustodialStakingActivitySummaryItem.trailingStrikethrough() = when (status) {
    StakingState.REFUNDED,
    StakingState.REJECTED,
    StakingState.FAILED -> true
    else -> false
}

internal fun CustodialStakingActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColor = when (status) {
        StakingState.COMPLETE -> ActivityTextColor.Title
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun CustodialStakingActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.StringValue(fiatValue?.toStringWithSymbol() ?: "--"),
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
