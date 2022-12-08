package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.earn.domain.models.interest.InterestState
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate
import info.blockchain.wallet.multiaddress.TransactionSummary

@DrawableRes internal fun CustodialInterestActivitySummaryItem.iconSummary(): Int {
    return when (status) {
        InterestState.COMPLETE -> when (type) {
            TransactionSummary.TransactionType.DEPOSIT -> R.drawable.ic_activity_buy
            TransactionSummary.TransactionType.INTEREST_EARNED -> R.drawable.ic_activity_rewards
            TransactionSummary.TransactionType.WITHDRAW -> R.drawable.ic_activity_sell
            else -> R.drawable.ic_activity_buy
        }
        else -> R.drawable.ic_activity_rewards
    }
}

internal fun CustodialInterestActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                TransactionSummary.TransactionType.DEPOSIT -> R.string.tx_title_added
                TransactionSummary.TransactionType.WITHDRAW -> R.string.tx_title_withdrawn
                TransactionSummary.TransactionType.INTEREST_EARNED -> R.string.tx_title_rewards
                else -> R.string.tx_title_transferred
            },
            args = listOf(account.currency.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialInterestActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColor = when (status) {
        InterestState.REJECTED,
        InterestState.REFUNDED -> ActivityTextColor.Warning
        InterestState.FAILED -> ActivityTextColor.Error
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = when (status) {
            InterestState.COMPLETE -> TextValue.StringValue(date.toFormattedDate())
            InterestState.PENDING,
            InterestState.PROCESSING,
            InterestState.MANUAL_REVIEW -> TextValue.IntResValue(R.string.activity_state_pending)
            InterestState.FAILED -> TextValue.IntResValue(R.string.activity_state_failed)
            InterestState.CLEARED -> TextValue.IntResValue(R.string.activity_state_cleared)
            InterestState.REFUNDED -> TextValue.IntResValue(R.string.activity_state_refunded)
            InterestState.REJECTED -> TextValue.IntResValue(R.string.activity_state_rejected)
            InterestState.UNKNOWN -> TextValue.IntResValue(R.string.activity_state_unknown)
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun CustodialInterestActivitySummaryItem.trailingStrikethrough() = when (status) {
    InterestState.REFUNDED,
    InterestState.REJECTED,
    InterestState.FAILED -> true
    else -> false
}

internal fun CustodialInterestActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColor = when (status) {
        InterestState.COMPLETE -> ActivityTextColor.Title
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

// todo(othman) find a way to get fiat because it's fetched by separate api
internal fun CustodialInterestActivitySummaryItem.trailingSubtitle(): ActivityStackView {
        return ActivityStackView.Text(
            value = TextValue.StringValue(fiatValue?.toStringWithSymbol() ?: "--"),
            style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
        )

//    return ActivityStackView.Text(
//        value = TextValue.StringValue("not implemented"),
//        style = basicSubtitleStyle
//    )
}
