package com.blockchain.home.presentation.activity.list.custodial.mappers

import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.image.LocalLogo
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CurrencyType
import java.util.Date

internal fun TradeActivitySummaryItem.isSwapPair(): Boolean = currencyPair.source.type == CurrencyType.CRYPTO &&
    currencyPair.destination.type == CurrencyType.CRYPTO && currencyPair.source != currencyPair.destination

internal fun TradeActivitySummaryItem.isSellingPair(): Boolean =
    currencyPair.source.type == CurrencyType.CRYPTO && currencyPair.destination.type == CurrencyType.FIAT

internal fun TradeActivitySummaryItem.iconSummary(): LocalLogo {
    return when {
        isSwapPair() -> LocalLogo.Swap
        isSellingPair() -> LocalLogo.Sell
        else -> error("unsupported")
    }
}

internal fun TradeActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value =
        when {
            isSwapPair() -> TextValue.IntResValue(
                value = com.blockchain.stringResources.R.string.tx_title_swapped,
                args = listOf(
                    currencyPair.source.displayTicker,
                    currencyPair.destination.displayTicker
                )
            )
            isSellingPair() -> TextValue.IntResValue(
                value = com.blockchain.stringResources.R.string.tx_title_sold,
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
    val color: ActivityTextColor = when (state) {
        CustodialOrderState.EXPIRED,
        CustodialOrderState.CANCELED -> ActivityTextColor.Warning
        CustodialOrderState.FAILED -> ActivityTextColor.Error
        else -> ActivityTextColor.Muted
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
    val color: ActivityTextColor = when (state) {
        CustodialOrderState.FINISHED -> ActivityTextColor.Title
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(fiatValue.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun TradeActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = when {
            isSwapPair() -> TextValue.StringValue(value.toStringWithSymbol())
            isSellingPair() -> TextValue.StringValue(receivingValue.toStringWithSymbol())
            else -> error("unsupported")
        },
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
