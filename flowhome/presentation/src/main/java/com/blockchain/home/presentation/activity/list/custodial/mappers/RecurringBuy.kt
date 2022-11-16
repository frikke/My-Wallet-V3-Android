package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.StringRes
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.custodial.list.basicSubtitleStyle
import com.blockchain.home.presentation.activity.custodial.list.basicTitleStyle
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.utils.toFormattedDate
import java.util.Date

@StringRes internal fun RecurringBuyActivitySummaryItem.icon(): Int {
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
        OrderState.FINISHED,
        OrderState.AWAITING_FUNDS,
        OrderState.PENDING_CONFIRMATION,
        OrderState.PENDING_EXECUTION,
        OrderState.UNINITIALISED,
        OrderState.INITIALISED,
        OrderState.UNKNOWN -> ActivityTextColorState.Muted
        OrderState.CANCELED -> ActivityTextColorState.Warning
        OrderState.FAILED -> ActivityTextColorState.Error
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
                    RecurringBuyFailureReason.INSUFFICIENT_FUNDS -> R.string.recurring_buy_insufficient_funds_short_error
                    else -> R.string.recurring_buy_short_error
                }
            )
            OrderState.UNINITIALISED,
            OrderState.INITIALISED,
            OrderState.UNKNOWN -> TextValue.StringValue("")
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

internal fun RecurringBuyActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColorState = when (transactionState) {
        OrderState.FINISHED -> ActivityTextColorState.Title
        else -> ActivityTextColorState.Muted
    }

    val strikethrough: Boolean = when (transactionState) {
        OrderState.FINISHED,
        OrderState.AWAITING_FUNDS,
        OrderState.PENDING_CONFIRMATION,
        OrderState.PENDING_EXECUTION,
        OrderState.UNINITIALISED,
        OrderState.INITIALISED,
        OrderState.UNKNOWN -> false
        OrderState.CANCELED,
        OrderState.FAILED -> true
    }

    return ActivityStackView.Text(
        value = when (transactionState) {
            OrderState.FINISHED -> TextValue.StringValue(value.toStringWithSymbol())
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION,
            OrderState.PENDING_CONFIRMATION,
            OrderState.CANCELED,
            OrderState.FAILED -> TextValue.StringValue(fundedFiat.toStringWithSymbol())
            OrderState.UNINITIALISED,
            OrderState.INITIALISED,
            OrderState.UNKNOWN -> TextValue.StringValue("")
        },
        style = basicTitleStyle.copy(color = color, strikethrough = strikethrough)
    )
}

internal fun RecurringBuyActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    val strikethrough: Boolean = when (transactionState) {
        OrderState.FINISHED,
        OrderState.AWAITING_FUNDS,
        OrderState.PENDING_CONFIRMATION,
        OrderState.PENDING_EXECUTION,
        OrderState.UNINITIALISED,
        OrderState.INITIALISED,
        OrderState.UNKNOWN -> false
        OrderState.CANCELED,
        OrderState.FAILED -> true
    }

    return ActivityStackView.Text(
        value = when (transactionState) {
            OrderState.FINISHED -> TextValue.StringValue(fundedFiat.toStringWithSymbol())
            else -> TextValue.StringValue("")
        },
        style = basicSubtitleStyle.copy(strikethrough = strikethrough)
    )
}
