package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityLocalIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate

internal fun CustodialTransferActivitySummaryItem.iconSummary(): ActivityLocalIcon {
    return when (type) {
        TransactionType.DEPOSIT -> ActivityLocalIcon.Receive
        TransactionType.WITHDRAWAL -> ActivityLocalIcon.Sell
    }
}

internal fun CustodialTransferActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                TransactionType.DEPOSIT -> com.blockchain.stringResources.R.string.tx_title_received
                TransactionType.WITHDRAWAL -> com.blockchain.stringResources.R.string.tx_title_withdrawn
            },
            args = listOf(account.currency.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialTransferActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        TransactionState.COMPLETED,
        TransactionState.MANUAL_REVIEW -> ActivityTextColor.Muted

        TransactionState.PENDING -> ActivityTextColor.Muted
        TransactionState.FAILED -> ActivityTextColor.Error
    }

    return ActivityStackView.Text(
        value = when (state) {
            TransactionState.COMPLETED,
            TransactionState.MANUAL_REVIEW,
            TransactionState.PENDING -> TextValue.StringValue(date.toFormattedDate())

            TransactionState.FAILED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_failed
            )
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun CustodialTransferActivitySummaryItem.trailingStrikethrough() = when (state) {
    TransactionState.FAILED -> true
    else -> false
}

internal fun CustodialTransferActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        TransactionState.COMPLETED -> ActivityTextColor.Title
        TransactionState.PENDING,
        TransactionState.MANUAL_REVIEW,
        TransactionState.FAILED -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(fiatValue.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun CustodialTransferActivitySummaryItem.trailingSubtitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
    )
}
