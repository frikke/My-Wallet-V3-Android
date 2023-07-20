package com.blockchain.home.presentation.activity.list.custodial.mappers

import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.coincore.toStringWithSymbolOrLessThanOnePenny
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.earn.domain.models.EarnRewardsState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.image.LocalLogo
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary

internal fun CustodialStakingActivitySummaryItem.iconSummary(): LocalLogo {
    return when (state) {
        EarnRewardsState.COMPLETE -> when (type) {
            TransactionSummary.TransactionType.DEPOSIT -> LocalLogo.Buy
            TransactionSummary.TransactionType.INTEREST_EARNED -> LocalLogo.StakingRewards
            TransactionSummary.TransactionType.WITHDRAW -> LocalLogo.Sell
            else -> LocalLogo.Buy
        }

        else -> LocalLogo.StakingRewards
    }
}

internal fun CustodialStakingActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                TransactionSummary.TransactionType.DEPOSIT ->
                    com.blockchain.stringResources.R.string.tx_title_staked

                TransactionSummary.TransactionType.WITHDRAW ->
                    com.blockchain.stringResources.R.string.tx_title_stake_withdrawn

                TransactionSummary.TransactionType.INTEREST_EARNED ->
                    com.blockchain.stringResources.R.string.tx_title_earned

                else -> com.blockchain.stringResources.R.string.tx_title_transferred
            },
            args = listOf(account.currency.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialStakingActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        EarnRewardsState.REJECTED,
        EarnRewardsState.REFUNDED -> ActivityTextColor.Warning

        EarnRewardsState.FAILED -> ActivityTextColor.Error
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = when (state) {
            EarnRewardsState.COMPLETE -> TextValue.StringValue(date.toFormattedDate())
            EarnRewardsState.PENDING,
            EarnRewardsState.PROCESSING,
            EarnRewardsState.MANUAL_REVIEW -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_pending
            )

            EarnRewardsState.FAILED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_failed
            )

            EarnRewardsState.CLEARED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_cleared
            )

            EarnRewardsState.REFUNDED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_refunded
            )

            EarnRewardsState.REJECTED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_rejected
            )

            EarnRewardsState.UNKNOWN -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_unknown
            )
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun CustodialStakingActivitySummaryItem.trailingStrikethrough() = when (state) {
    EarnRewardsState.REFUNDED,
    EarnRewardsState.REJECTED,
    EarnRewardsState.FAILED -> true

    else -> false
}

internal fun CustodialStakingActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        EarnRewardsState.COMPLETE -> ActivityTextColor.Title
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(
            fiatValue?.let {
                (it as FiatValue).toStringWithSymbolOrLessThanOnePenny()
            } ?: "--"
        ),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun CustodialStakingActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
