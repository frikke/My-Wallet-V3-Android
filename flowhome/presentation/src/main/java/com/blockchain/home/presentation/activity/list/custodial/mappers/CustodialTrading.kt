package com.blockchain.home.presentation.activity.list.custodial.mappers

import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityLocalIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate
import java.util.Date

internal fun CustodialTradingActivitySummaryItem.iconSummary(): ActivityLocalIcon {
    return when (type) {
        OrderType.BUY,
        OrderType.RECURRING_BUY -> ActivityLocalIcon.Buy
        OrderType.SELL -> ActivityLocalIcon.Sell
    }
}

internal fun CustodialTradingActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                OrderType.BUY,
                OrderType.RECURRING_BUY -> com.blockchain.stringResources.R.string.tx_title_bought
                OrderType.SELL -> com.blockchain.stringResources.R.string.tx_title_sold
            },
            args = listOf(account.currency.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialTradingActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        OrderState.CANCELED -> ActivityTextColor.Warning
        OrderState.FAILED -> ActivityTextColor.Error
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = when (state) {
            OrderState.FINISHED -> TextValue.StringValue(Date(timeStampMs).toFormattedDate())
            OrderState.UNINITIALISED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_uninitialised
            )
            OrderState.INITIALISED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_initialised
            )
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION,
            OrderState.PENDING_CONFIRMATION -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_pending
            )
            OrderState.UNKNOWN -> TextValue.IntResValue(com.blockchain.stringResources.R.string.activity_state_unknown)
            OrderState.CANCELED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_canceled
            )
            OrderState.FAILED -> TextValue.IntResValue(com.blockchain.stringResources.R.string.activity_state_failed)
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun CustodialTradingActivitySummaryItem.trailingStrikethrough() = when (state) {
    OrderState.CANCELED,
    OrderState.FAILED -> true
    else -> false
}

internal fun CustodialTradingActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        OrderState.FINISHED -> ActivityTextColor.Title
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(fundedFiat.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun CustodialTradingActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
