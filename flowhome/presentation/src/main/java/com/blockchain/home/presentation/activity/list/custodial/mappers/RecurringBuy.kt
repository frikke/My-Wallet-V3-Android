package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.utils.toFormattedDate
import java.util.Date

@DrawableRes internal fun RecurringBuyActivitySummaryItem.iconSummary(): Int {
    return R.drawable.ic_activity_buy
}

internal fun RecurringBuyActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = R.string.tx_title_bought,
            args = listOf(asset.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun RecurringBuyActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColorState = when (transactionState) {
        OrderState.CANCELED -> ActivityTextColorState.Warning
        OrderState.FAILED -> ActivityTextColorState.Error
        else -> ActivityTextColorState.Muted
    }

    return ActivityStackView.Text(
        value = when (transactionState) {
            OrderState.FINISHED -> TextValue.StringValue(Date(timeStampMs).toFormattedDate())
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION,
            OrderState.PENDING_CONFIRMATION -> TextValue.IntResValue(R.string.activity_state_pending)
            OrderState.CANCELED -> TextValue.IntResValue(R.string.activity_state_canceled)
            OrderState.FAILED -> TextValue.IntResValue(
                when (failureReason) {
                    RecurringBuyFailureReason.INSUFFICIENT_FUNDS -> {
                        R.string.recurring_buy_insufficient_funds_short_error
                    }
                    else -> R.string.recurring_buy_short_error
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
    val color: ActivityTextColorState = when (transactionState) {
        OrderState.FINISHED -> ActivityTextColorState.Title
        else -> ActivityTextColorState.Muted
    }

    return ActivityStackView.Text(
        value = when (transactionState) {
            OrderState.FINISHED -> TextValue.StringValue(value.toStringWithSymbol())
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION,
            OrderState.PENDING_CONFIRMATION,
            OrderState.CANCELED,
            OrderState.FAILED -> TextValue.StringValue(fundedFiat.toStringWithSymbol())
            else -> TextValue.StringValue("")
        },
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun RecurringBuyActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = when (transactionState) {
            OrderState.FINISHED -> TextValue.StringValue(fundedFiat.toStringWithSymbol())
            else -> TextValue.StringValue("")
        },
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
