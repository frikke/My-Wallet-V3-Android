package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.StringRes
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.custodial.list.basicSubtitleStyle
import com.blockchain.home.presentation.activity.custodial.list.basicTitleStyle
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.utils.toFormattedDate
import java.util.Date

@StringRes internal fun CustodialTradingActivitySummaryItem.icon(): Int {
    return when (type) {
        OrderType.BUY,
        OrderType.RECURRING_BUY -> R.drawable.ic_activity_buy
        OrderType.SELL -> R.drawable.ic_activity_sell
    }
}

internal fun CustodialTradingActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                OrderType.BUY,
                OrderType.RECURRING_BUY -> R.string.tx_title_bought
                OrderType.SELL -> R.string.tx_title_sold
            },
            args = listOf(asset.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialTradingActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColorState = when (status) {
        OrderState.CANCELED -> ActivityTextColorState.Warning
        OrderState.FAILED -> ActivityTextColorState.Error
        else -> ActivityTextColorState.Muted
    }

    return ActivityStackView.Text(
        value = when (status) {
            OrderState.FINISHED -> TextValue.StringValue(Date(timeStampMs).toFormattedDate())
            OrderState.UNINITIALISED -> TextValue.IntResValue(R.string.activity_state_uninitialised)
            OrderState.INITIALISED -> TextValue.IntResValue(R.string.activity_state_initialised)
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION,
            OrderState.PENDING_CONFIRMATION -> TextValue.IntResValue(R.string.activity_state_pending)
            OrderState.UNKNOWN -> TextValue.IntResValue(R.string.activity_state_unknown)
            OrderState.CANCELED -> TextValue.IntResValue(R.string.activity_state_canceled)
            OrderState.FAILED -> TextValue.IntResValue(R.string.activity_state_failed)
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun CustodialTradingActivitySummaryItem.trailingStrikethrough() = when (status) {
    OrderState.CANCELED,
    OrderState.FAILED -> true
    else -> false
}

internal fun CustodialTradingActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColorState = when (status) {
        OrderState.FINISHED -> ActivityTextColorState.Title
        else -> ActivityTextColorState.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun CustodialTradingActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.StringValue(fundedFiat.toStringWithSymbol()),
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
