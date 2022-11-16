package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.StringRes
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.custodial.list.basicSubtitleStyle
import com.blockchain.home.presentation.activity.custodial.list.basicTitleStyle
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CurrencyType
import java.util.Date

private fun TradeActivitySummaryItem.isSwapPair(): Boolean = currencyPair.source.type == CurrencyType.CRYPTO &&
    currencyPair.destination.type == CurrencyType.CRYPTO && currencyPair.source != currencyPair.destination

private fun TradeActivitySummaryItem.isSellingPair(): Boolean =
    currencyPair.source.type == CurrencyType.CRYPTO && currencyPair.destination.type == CurrencyType.FIAT

@StringRes internal fun TradeActivitySummaryItem.icon(): Int {
    return when {
        isSwapPair() -> R.drawable.ic_activity_swap
        isSellingPair() -> R.drawable.ic_activity_sell
        else -> error("unsupported")
    }
}

internal fun TradeActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value =
        when {
            isSwapPair() -> TextValue.IntResValue(
                value = R.string.tx_title_swapped,
                args = listOf(
                    currencyPair.source.displayTicker,
                    currencyPair.destination.displayTicker
                )
            )
            isSellingPair() -> TextValue.IntResValue(
                value = R.string.tx_title_sold,
                args = listOf(
                    currencyPair.source.displayTicker
                )
            )
            else -> error("unsupported")
        },
        style = basicTitleStyle
    )
}

internal fun TradeActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColorState = when (state) {
        CustodialOrderState.EXPIRED,
        CustodialOrderState.CANCELED -> ActivityTextColorState.Warning
        CustodialOrderState.FAILED -> ActivityTextColorState.Error
        else -> ActivityTextColorState.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(Date(timeStampMs).toFormattedDate()),
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun TradeActivitySummaryItem.trailingStrikethrough() = when (state) {
    CustodialOrderState.EXPIRED,
    CustodialOrderState.CANCELED,
    CustodialOrderState.FAILED -> true
    else -> false
}

internal fun TradeActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColorState = when (state) {
        CustodialOrderState.FINISHED -> ActivityTextColorState.Title
        else -> ActivityTextColorState.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun TradeActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = when {
            isSwapPair() -> TextValue.StringValue(fiatValue.toStringWithSymbol())
            isSellingPair() -> TextValue.StringValue(receivingValue.toStringWithSymbol())
            else -> error("unsupported")
        },
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
