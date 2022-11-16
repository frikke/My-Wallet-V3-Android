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
        isSellingPair() ->R.drawable.ic_activity_sell
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
        CustodialOrderState.CREATED,
        CustodialOrderState.PENDING_CONFIRMATION,
        CustodialOrderState.PENDING_LEDGER,
        CustodialOrderState.PENDING_EXECUTION,
        CustodialOrderState.PENDING_DEPOSIT,
        CustodialOrderState.FINISH_DEPOSIT,
        CustodialOrderState.PENDING_WITHDRAWAL,
        CustodialOrderState.FINISHED,
        CustodialOrderState.UNKNOWN -> ActivityTextColorState.Muted
        CustodialOrderState.EXPIRED,
        CustodialOrderState.CANCELED -> ActivityTextColorState.Warning
        CustodialOrderState.FAILED -> ActivityTextColorState.Error
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(Date(timeStampMs).toFormattedDate()),
        style = basicSubtitleStyle.copy(color = color)
    )
}

internal fun TradeActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColorState = when (state) {
        CustodialOrderState.FINISHED -> ActivityTextColorState.Title
        else -> ActivityTextColorState.Muted
    }

    val strikethrough: Boolean = when (state) {
        CustodialOrderState.CREATED,
        CustodialOrderState.PENDING_CONFIRMATION,
        CustodialOrderState.PENDING_LEDGER,
        CustodialOrderState.PENDING_EXECUTION,
        CustodialOrderState.PENDING_DEPOSIT,
        CustodialOrderState.FINISH_DEPOSIT,
        CustodialOrderState.PENDING_WITHDRAWAL,
        CustodialOrderState.FINISHED,
        CustodialOrderState.UNKNOWN -> false
        CustodialOrderState.EXPIRED,
        CustodialOrderState.CANCELED,
        CustodialOrderState.FAILED -> true
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = strikethrough)
    )
}

internal fun TradeActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    val strikethrough: Boolean = when (state) {
        CustodialOrderState.CREATED,
        CustodialOrderState.PENDING_CONFIRMATION,
        CustodialOrderState.PENDING_LEDGER,
        CustodialOrderState.PENDING_EXECUTION,
        CustodialOrderState.PENDING_DEPOSIT,
        CustodialOrderState.FINISH_DEPOSIT,
        CustodialOrderState.PENDING_WITHDRAWAL,
        CustodialOrderState.FINISHED,
        CustodialOrderState.UNKNOWN -> false
        CustodialOrderState.EXPIRED,
        CustodialOrderState.CANCELED,
        CustodialOrderState.FAILED -> true
    }

    return ActivityStackView.Text(
        value = when {
            isSwapPair() -> TextValue.StringValue(fiatValue.toStringWithSymbol())
            isSellingPair() -> TextValue.StringValue(receivingValue.toStringWithSymbol())
            else -> error("unsupported")
        },
        style = basicSubtitleStyle.copy(strikethrough = strikethrough)
    )
}
