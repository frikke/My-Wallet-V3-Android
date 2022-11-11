package com.blockchain.home.presentation.activity.custodial.list.mappers

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

// todo tint
@StringRes internal fun CustodialTradingActivitySummaryItem.icon(): Int {
    return when (status) {
        OrderState.FINISHED -> if (type == OrderType.BUY) R.drawable.ic_tx_buy else R.drawable.ic_tx_sell
        OrderState.AWAITING_FUNDS,
        OrderState.PENDING_CONFIRMATION,
        OrderState.PENDING_EXECUTION -> R.drawable.ic_tx_confirming
        OrderState.UNINITIALISED, // should not see these next ones ATM
        OrderState.INITIALISED,
        OrderState.UNKNOWN,
        OrderState.CANCELED,
        OrderState.FAILED -> if (type == OrderType.BUY) R.drawable.ic_tx_buy else R.drawable.ic_tx_sell
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

internal fun CustodialTradingActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColorState = when (status) {
        OrderState.FINISHED -> ActivityTextColorState.Title
        OrderState.AWAITING_FUNDS,
        OrderState.PENDING_CONFIRMATION,
        OrderState.PENDING_EXECUTION,
        OrderState.UNINITIALISED,
        OrderState.INITIALISED,
        OrderState.UNKNOWN,
        OrderState.CANCELED,
        OrderState.FAILED -> ActivityTextColorState.Muted
    }

    val strikethrough: Boolean = when (status) {
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
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = strikethrough)
    )
}

internal fun CustodialTradingActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    val strikethrough: Boolean = when (status) {
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
        value = TextValue.StringValue(fundedFiat.toStringWithSymbol()),
        style = basicSubtitleStyle.copy(strikethrough = strikethrough)
    )
}
