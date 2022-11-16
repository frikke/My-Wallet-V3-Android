package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.StringRes
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.custodial.list.basicSubtitleStyle
import com.blockchain.home.presentation.activity.custodial.list.basicTitleStyle
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.utils.toFormattedDate

@StringRes internal fun CustodialTransferActivitySummaryItem.icon(): Int {
    return when (type) {
        TransactionType.DEPOSIT -> R.drawable.ic_activity_receive
        TransactionType.WITHDRAWAL -> R.drawable.ic_activity_send
    }
}

internal fun CustodialTransferActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                TransactionType.DEPOSIT -> R.string.tx_title_received
                TransactionType.WITHDRAWAL -> R.string.tx_title_sent
            },
            args = listOf(asset.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialTransferActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColorState = when (state) {
        TransactionState.COMPLETED,
        TransactionState.PENDING -> ActivityTextColorState.Muted
        TransactionState.FAILED -> ActivityTextColorState.Error
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(date.toFormattedDate()),
        style = basicSubtitleStyle.copy(color = color)
    )
}

internal fun CustodialTransferActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColorState = when (state) {
        TransactionState.COMPLETED -> ActivityTextColorState.Title
        TransactionState.PENDING,
        TransactionState.FAILED -> ActivityTextColorState.Muted
    }

    val strikethrough: Boolean = when (state) {
        TransactionState.COMPLETED,
        TransactionState.PENDING -> false
        TransactionState.FAILED -> true
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = strikethrough)
    )
}

internal fun CustodialTransferActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    val strikethrough: Boolean = when (state) {
        TransactionState.COMPLETED,
        TransactionState.PENDING -> false
        TransactionState.FAILED -> true
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(fiatValue.toStringWithSymbol()),
        style = basicSubtitleStyle.copy(strikethrough = strikethrough)
    )
}
