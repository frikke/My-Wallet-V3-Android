package com.blockchain.home.presentation.activity.list.custodial.mappers

import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityLocalIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate
import java.util.Date

internal fun RecurringBuyActivitySummaryItem.iconSummary(): ActivityLocalIcon {
    return ActivityLocalIcon.Buy
}

internal fun RecurringBuyActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = com.blockchain.stringResources.R.string.tx_title_bought,
            args = listOf(account.currency.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun RecurringBuyActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColor = when (transactionState) {
        OrderState.CANCELED -> ActivityTextColor.Warning
        OrderState.FAILED -> ActivityTextColor.Error
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = when (transactionState) {
            OrderState.FINISHED -> TextValue.StringValue(Date(timeStampMs).toFormattedDate())
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION,
            OrderState.PENDING_CONFIRMATION -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_pending
            )
            OrderState.CANCELED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_canceled
            )
            OrderState.FAILED -> TextValue.IntResValue(
                when (failureReason) {
                    RecurringBuyFailureReason.INSUFFICIENT_FUNDS -> {
                        com.blockchain.stringResources.R.string.recurring_buy_insufficient_funds_short_error
                    }
                    else -> com.blockchain.stringResources.R.string.recurring_buy_short_error
                }
            )
            else -> TextValue.StringValue("")
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun RecurringBuyActivitySummaryItem.trailingStrikethrough() = when (transactionState) {
    OrderState.CANCELED,
    OrderState.FAILED -> true
    else -> false
}

internal fun RecurringBuyActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColor = when (transactionState) {
        OrderState.FINISHED -> ActivityTextColor.Title
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(fundedFiat.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun RecurringBuyActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
